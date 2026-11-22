local current = redis.call('HMGET', KEYS[1], 'seq', 'price', 'volume', 'ts')
local currentSeq = tonumber(current[1]) or -1
local incomingSeq = tonumber(ARGV[1])

if incomingSeq >= currentSeq then
  redis.call('HMSET', KEYS[1],
    'seq', ARGV[1],
    'price', ARGV[2],
    'volume', ARGV[3],
    'ts', ARGV[4])
  return 1
end
return 0
