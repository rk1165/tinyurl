### TinyURL

- We have all seen the classic system design interview question of creating a TinyURL service. This is my attempt to
  create one as a learning exercise.

---

### 1. Requirements & Capacity Planning

- **Traffic:** 100 million new URLs generated per month.
- **Read/Write Ratio:** 100:1 (For every 1 URL shortened, it is clicked 100 times).
- **Lifespan:** We store URLs for 5 years.

**Back-of-the-envelope calculation:**

- **Writes:** 100M requests/month $\approx$ 40 requests/second.
- **Reads:** 100M $\times$ 100 = 10B requests/month $\approx$ 4,000 requests/second.
- **Storage:** If one record is 500 bytes: 100M $\times$ 12 months $\times$ 5 years $\times$ 500 bytes $\approx$ 3 TB of
  data

---

### 2. Generating the Short Key

- On the surface it looks like a simple problem where we have a giant Map of key:value pairs where keys are hashes of
  long urls and values are the long urls themselves.
- To create such map we can use hashing algorithms like - MD5/SHA and take the first 6-7 characters.
- Problem with the above approach is that hashing produces collisions. Different Long URLs might result in the same
  first 6-7 characters which can overwrite the existing combination
- Another approach is that, we use a Key Generation Service (KGS) which will keep giving unique keys and then we Base62
  encode it.
- Another thing to think about is how long the short url should be. Using base62 encoding, if we consider 7 characters
  it gives us $62^7$ combinations $\approx$ 3.5 trillion combinations. Which is more than enough for 5 years of data

---

### 3. High-Level Architecture

**Step-by-Step Flow:**

1. **Client** sends a `POST` request with the Long URL.
2. **Web Server** talks to a unique ID generator
3. **Worker** converts the ID to Base62.
4. **DB** stores the mapping: `{ shortUrl: "abc", longUrl: "http://..." }`.
5. **Client** receives the short URL.
6. **On Redirect:** Client hits the short URL $\rightarrow$ Load Balancer $\rightarrow$ Cache (Redis) $\rightarrow$
   Database $\rightarrow$ Returns HTTP 301/302 to Client.

---

### 4. API Design

- We need two primary API endpoints

**1. Shorten URL**

- **Endpoint:** `POST /api/v1/tinyurl/shorten`
- **Payload:** `{ "longUrl": "https://www.google.com/..." }`
- **Response:** `{ "shortUrl": "https://tiny.url/xyz123" }` - 201 created if the long url is not present otherwise
  existing shortUrl with 200 OK response

**2. Redirect URL**

- **Endpoint:** `GET /api/v1/tinyurl/{shortUrl}`
- **Response:** HTTP Redirect (Status 301 or 302) to the Long URL or 404 if not found.

---

### 5. Critical Problems & Approaches

#### "Distributed ID" Problem

If we have multiple web servers running, they cannot simply use a local counter (e.g., `count++`) to generate IDs,
because two
servers might generate ID `100` at the same time, causing a collision. So, we need something of the sort which is unique
globally

- We can use a central DB's Auto-Increment to generate IDs. The tradeoff is that it can become a bottleneck and Single
  Point of Failure
- Another approach is that we use a Key Generation Service (KGS) like Snowflake which can generate keys on the fly.

#### 301 vs. 302 Redirects

Which HTTP status code should we return when a user clicks the short link?

- If we return **301 (Permanent Redirect):** The browser will cache the redirection. Next time when the user clicks the
  link, the browser goes
  _directly_ to the long URL without hitting our server.
    - _Pro:_ Reduces load on your server.
    - _Con:_ **Analytics loss.** We cannot track how many times the link was clicked after the first time.
- If we return **302 (Temporary Redirect):** The browser will hit our server every single time.
    - _Pro:_ Accurate analytics.
    - _Con:_ Higher server load.

#### Getting the long url from short one

- If we used only database for looking up the long url it can increase the latency and load on our DB.
- Since URLs don't change often, we can cache them and introduce Redis check before hitting the DB.
- We can use **LRU (Least Recently User)** algorithm for eviction policy.

#### Optimization

- One optimization is we can use `batch` call of the idgenerator service and keep the keys in memory / redis. Then when the shorten API call is made we can just get those keys from memory/redis.

---

### Data Model

- We are using short_url itself as the primary key and have created an index on long_url so that we can quickly look up
  if we have already created the short url

| short_url | long_url              | created_at   | click_count | 
|:----------|:----------------------|:-------------|:------------|
| `3hK8`    | `http://google.com`   | `2025-12-08` | 10          |
| `3hK9`    | `http://facebook.com` | `2025-12-08` | 20          |

### Implementation

- We are using Snowflake id generator as a KGS which I have implemented [here](https://github.com/rk1165/idgenerator)
- One problem is that shortUrl being generated is having a length of 10 characters. I haven't given much thought on
  reducing it to 7 chars.
- There is a `ClickTrackingService` which flushes the click count periodically from Redis to our MySQL DB.

### Running the service

- There's a Makefile which has commands to run the services locally.

### Load Test

- This is not a highly optimized service, but I ran a simple load test on my local machine and the results are shown
  below.
- Since it was difficult to generate massive load I tried using a list of 100_000 URLs for insertion which is present under `load_test` in `domains.txt`
- `insert_load.lua` is used for distributing the load across the 12 threads
- Then we can use `mysql -u root -N -e "SELECT short_url from tinyurl_db.tiny_urls;" > short_urls.txt` to dump the short urls which got generated into a file and concatenate it multiple times and run the `shortUrl` API load test.
- Below are the insertion load stats

```
Running 1m test @ http://localhost:8080
  12 threads and 200 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    45.20ms   53.88ms 704.22ms   89.57%
    Req/Sec   511.62    294.49     1.51k    61.01%
  Latency Distribution
     50%   26.25ms
     75%   48.30ms
     90%  103.75ms
     99%  251.02ms
  350869 requests in 1.00m, 50.26MB read
  Non-2xx or 3xx responses: 26
Requests/sec:   5838.18
Transfer/sec:    856.33KB
```

Following are the load stats when reading the short url 

#### When using only DB for lookup

```
Running 1m test @ http://localhost:8080
  12 threads and 400 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    51.11ms   34.12ms 495.45ms   92.65%
    Req/Sec   722.36    176.66     2.44k    88.62%
  Latency Distribution
     50%   41.62ms
     75%   48.55ms
     90%   68.50ms
     99%  216.35ms
  517341 requests in 1.00m, 80.48MB read
Requests/sec:   8609.06
Transfer/sec:      1.34MB
```

#### When using Redis for lookup

```
Running 1m test @ http://localhost:8080
  12 threads and 400 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    23.41ms   29.66ms 495.06ms   94.86%
    Req/Sec     1.82k   696.83     5.79k    75.96%
  Latency Distribution
     50%   15.06ms
     75%   17.71ms
     90%   44.72ms
     99%  176.23ms
  1311444 requests in 1.00m, 204.03MB read
Requests/sec:  21819.65
Transfer/sec:      3.39MB
```

#### When using Redis + Virtual Threads

```
Running 1m test @ http://localhost:8080
  12 threads and 400 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    30.82ms   45.51ms 822.35ms   90.60%
    Req/Sec     1.84k     1.70k   23.52k    93.88%
  Latency Distribution
     50%   14.40ms
     75%   21.78ms
     90%   71.94ms
     99%  241.53ms
  1338450 requests in 1.00m, 208.23MB read
Requests/sec:  22272.30
Transfer/sec:      3.47MB
```

### Tasks remaining - if I find the time

- Add Junit Tests and Jacoco coverage
- Add checkstyle.xml, pmd-rules, spotbugs
- Add quality-metric-rules.json and quality-config.yaml
- Add Dockerfile
- Add metrics - prometheus / grafana
- Add docker-compose
- Add GitHub workflows
- Deploy on Digital Ocean - using ec2 like server
- Deploy on AWS - Using Fargate
- Try for Kubernetes deployment too
- Add Nginx and Load balancing
- Add Load Testing stats
- Export logs to Splunk like service
- Add OpenTelemetry for Distributed Tracing
- Add Authorization and Authentication and User Management
- Add Rate Limiting
- Dependency vulnerability scanning : OWASP Dependency-Check / Snyk / Trivy
- Add HTTPS
- Health Checks and Liveness probes
- Retry / Circuit Breaker / Bulkhead (Resilience4j)
- TestContainers - Spin up DB, Redis, Kafka for integration tests

### Remarks

- I have a suspicion that there might be some Race Condition happening in the shortening API endpoint when two threads
  are trying to insert the same URL because I saw the following Exception few times when performing the load test.
- If someone figures it out kindly let me know.

```java
2025-12-08 22:34:21.612 [tomcat-handler-161] ERROR o.a.c.c.C.[.[.[.[dispatcherServlet] - Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed: java.lang.NullPointerException] with root cause
java.lang.NullPointerException: null
at java.base/java.util.Objects.requireNonNull(Objects.java:233)
at java.base/java.util.ImmutableCollections$Map1.<init>(ImmutableCollections.java:1112)
at java.base/java.util.Map.of(Map.java:1365)
at com.tinyurl.controller.TinyUrlController.post(TinyUrlController.java:61)
```