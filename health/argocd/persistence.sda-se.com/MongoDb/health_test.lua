local io = require("io")
local yml = require "lyaml"

-- relative to repo root
local basePath = "./health/argocd/persistence.sda-se.com/MongoDb/"

function readAll(file)
  local f = assert(io.open(file, "rb"))
  local content = f:read("*all")
  f:close()
  return content
end

local testsSource = readAll(basePath .. "health_test.yaml")

local tests = yml.load(testsSource)
print(tests)

for i, test in ipairs(tests.tests) do
  print("Testing: " .. test.inputPath)
  local given = readAll(basePath .. test.inputPath)
  obj = yml.load(given)
  local result = dofile(basePath .. "health.lua")
  assert(result.status ~= nil, "Actual status is nil")
  assert ( result.status == test.healthStatus.status, "Expected status " .. test.healthStatus.status .. " does not match actual status " .. result.status)
  print("  Status asserted: " .. result.status)
  assert(result.message ~= nil, "Actual message is nil")
  assert ( result.message == test.healthStatus.message, "Expected message '" .. test.healthStatus.message .. "' does not match actual message '" .. result.message .. "'")
  print("  Message asserted: " .. result.message)
end

