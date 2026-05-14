using System.Text.Json;
using System.Text.Json.Serialization;
using Microsoft.AspNetCore.Mvc;
using WarehouseService.Api;
using WarehouseService.Application;
using WarehouseService.Infrastructure;

var builder = WebApplication.CreateBuilder(args);

builder.Services.ConfigureHttpJsonOptions(options =>
{
    options.SerializerOptions.Converters.Add(new JsonStringEnumConverter());
    options.SerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.CamelCase;
});

builder.Services.AddSingleton<IWarehouseRepository, InMemoryWarehouseRepository>();
builder.Services.AddScoped<GetInventoryUseCase>();
builder.Services.AddScoped<ReserveStockUseCase>();
builder.Services.AddScoped<CancelReservationUseCase>();
builder.Services.AddScoped<CommitReservationUseCase>();
builder.Services.AddHttpClient<IAuditSink, HttpAuditSink>();

builder.Services.AddProblemDetails();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

var app = builder.Build();

app.UseExceptionHandler();
app.UseStatusCodePages();
app.UseMiddleware<CorrelationIdMiddleware>();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.MapGet("/health", () => Results.Ok(new { service = "warehouse-service", status = "healthy" }));
app.MapGet("/", () => Results.Redirect("/swagger"));

app.MapGet("/inventory", (GetInventoryUseCase useCase) => Results.Ok(useCase.Execute()));

app.MapPost("/reservations", async (
    CreateReservationRequest request,
    ReserveStockUseCase useCase,
    IAuditSink audit,
    HttpContext context,
    CancellationToken cancellationToken) =>
{
    var correlationId = context.GetCorrelationId();
    var validationError = request.Validate();
    if (validationError is not null)
    {
        await audit.RecordAsync(correlationId, "WAREHOUSE_RESERVATION_REJECTED", "FAILURE", request, cancellationToken);
        return Results.ValidationProblem(validationError);
    }

    var result = await useCase.ExecuteAsync(request.ToCommand(correlationId), cancellationToken);
    if (!result.Success)
    {
        return Results.Conflict(new ProblemDetails
        {
            Title = "Insufficient stock",
            Detail = result.Message,
            Status = StatusCodes.Status409Conflict
        });
    }

    return Results.Created($"/reservations/{result.Reservation!.ReservationId}", result.Reservation);
});

app.MapDelete("/reservations/{reservationId:guid}", async (
    Guid reservationId,
    CancelReservationUseCase useCase,
    HttpContext context,
    CancellationToken cancellationToken) =>
{
    var reservation = await useCase.ExecuteAsync(reservationId, context.GetCorrelationId(), cancellationToken);
    return reservation is null ? Results.NotFound() : Results.Ok(reservation);
});

app.MapPost("/reservations/{reservationId:guid}/commit", async (
    Guid reservationId,
    CommitReservationUseCase useCase,
    HttpContext context,
    CancellationToken cancellationToken) =>
{
    var reservation = await useCase.ExecuteAsync(reservationId, context.GetCorrelationId(), cancellationToken);
    return reservation is null ? Results.NotFound() : Results.Ok(reservation);
});

app.Run();
