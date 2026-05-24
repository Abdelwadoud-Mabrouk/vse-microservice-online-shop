using AuditService.Api.DependencyInjection;
using AuditService.Api.Endpoints;
using AuditService.Api.Security;
using AuditService.Infrastructure.DependencyInjection;

var builder = WebApplication.CreateBuilder(args);

builder.AddAuditObservability();

builder.Services.AddAuditApplicationServices();
builder.Services.AddAuditPersistence(builder.Configuration);
builder.Services.AddAuditKafkaConsumers(builder.Configuration);

var app = builder.Build();

app.UseExceptionHandler();
app.UseStatusCodePages();
app.UseMiddleware<AdminBasicAuthMiddleware>();
app.UseDefaultFiles();
app.UseStaticFiles();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.MapSystemEndpoints();
app.MapAdminEndpoints();
app.MapAuditEndpoints();

app.Run();
