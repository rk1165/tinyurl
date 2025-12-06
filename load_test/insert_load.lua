-- Load URLs from file into a table
local urls = {}
local file = io.open("domains.txt", "r")
if file then
    for line in file:lines() do
        if line ~= "" then
            table.insert(urls, line)
        end
    end
    file:close()
else
    print("ERROR: Could not open file")
end

-- print("Loaded " .. #urls .. " URLs from file")

local thread_count = 0

function setup(thread)
    -- Assign each thread a unique ID
    thread:set("id", thread_count)
    thread_count = thread_count + 1
end

function init(args)
    thread_id = wrk.thread:get("id")
    
    -- Get total threads from command line argument (required!)
    -- Usage: wrk -t12 -c400 -d60s -s insert_load.lua http://localhost:8080 -- 12
    total_threads = tonumber(args[1])
    
    if total_threads == nil or total_threads == 0 then
        print("ERROR: You must pass the thread count as argument!")
        print("Usage: wrk -t12 -c400 -d60s -s insert_load.lua http://localhost:8080 -- 12")
        total_threads = 1
    end
    
    -- Safety check
    if #urls == 0 then
        print("ERROR: No URLs loaded! Check file path.")
    end
    
    -- Thread N will handle URLs at indices: N+1, N+1+total_threads, N+1+2*total_threads, ...
    -- This spreads each thread's URLs across the entire file
    request_count = 0
    max_requests = math.ceil(#urls / total_threads)
    
    print("Thread " .. thread_id .. " initialized: will handle ~" .. max_requests .. " URLs (total_threads=" .. total_threads .. ")")
end

function request()
    request_count = request_count + 1
    
    -- Calculate which URL this thread should request
    -- Thread 0: URLs 1, 13, 25... Thread 1: URLs 2, 14, 26... etc (for 12 threads)
    local index = thread_id + 1 + (request_count - 1) * total_threads
    
    -- If we've exhausted our URLs, wrap (will be duplicates, but harmless)
    if index > #urls then
        index = thread_id + 1  -- Restart from first URL for this thread
        request_count = 1
    end
    
    local url = urls[index]
    
    -- Guard against nil
    if url == nil then
        url = urls[1]
    end
    
    local body = '{"longUrl":"http://www.' .. url .. '"}'

    return wrk.format("POST", "/api/v1/tinyurl/shorten", {
        ["Content-Type"] = "application/json"
        }, body)
end
