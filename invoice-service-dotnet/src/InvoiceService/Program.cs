using InvoiceService.Api;
using InvoiceService.Api.DependencyInjection;
using InvoiceService.Api.Endpoints;
using InvoiceService.Infrastructure.DependencyInjection;

var builder = WebApplication.CreateBuilder(args);

builder.AddInvoiceObservability();

builder.Services.AddInvoiceApplicationServices();
builder.Services.AddInvoiceInfrastructure();

var app = builder.Build();

app.UseExceptionHandler();
app.UseStatusCodePages();
app.UseMiddleware<CorrelationIdMiddleware>();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.MapSystemEndpoints();
app.MapInvoiceEndpoints();

app.Run();
