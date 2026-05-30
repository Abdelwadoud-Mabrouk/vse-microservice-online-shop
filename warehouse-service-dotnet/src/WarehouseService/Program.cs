using WarehouseService.Api;
using WarehouseService.Api.DependencyInjection;
using WarehouseService.Api.Endpoints;
using WarehouseService.Infrastructure.DependencyInjection;

var builder = WebApplication.CreateBuilder(args);

builder.AddWarehouseObservability();

builder.Services.AddWarehouseApplicationServices();
builder.Services.AddWarehousePersistence(builder.Configuration);

var app = builder.Build();

await app.InitializeWarehouseDatabaseAsync();

app.UseExceptionHandler();
app.UseStatusCodePages();
app.UseMiddleware<CorrelationIdMiddleware>();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.MapSystemEndpoints();
app.MapWarehouseEndpoints();

app.Run();
