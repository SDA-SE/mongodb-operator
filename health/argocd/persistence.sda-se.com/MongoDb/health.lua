health_status={}
if obj.status ~= nil then
  if obj.status.conditions ~= nil and #(obj.status.conditions) > 0 then
    for i, condition in ipairs(obj.status.conditions) do
      if condition.status ~= "True" then
        health_status.status = "Degraded"
        health_status.message = condition.message
        return health_status
      end
    end
    if #(obj.status.conditions) < 3 then
      health_status.status = "Degraded"
      health_status.message = "Missing conditions."
      return health_status
    end
    health_status.status = "Healthy"
    health_status.message = "Database and secret created."
    return health_status
  end
end
health_status.status = "Progressing"
health_status.message = "Waiting for database creation."
return health_status