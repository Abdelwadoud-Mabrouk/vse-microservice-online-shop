using System.Text.Json;
using System.Text.Json.Serialization;
using AuditService.Api;
using AuditService.Application;
using AuditService.Infrastructure;

var builder = WebApplication.CreateBuilder(args);

builder.Services.ConfigureHttpJsonOptions(options =>
{
    options.SerializerOptions.Converters.Add(new JsonStringEnumConverter());
    options.SerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.CamelCase;
});

builder.Services.AddScoped<AppendAuditSnapshotUseCase>();
builder.Services.AddScoped<GetAuditTimelineUseCase>();
builder.Services.AddSingleton<IAuditSnapshotRepository, PostgresAuditSnapshotRepository>();

builder.Services.AddProblemDetails();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

var app = builder.Build();

app.UseExceptionHandler();
app.UseStatusCodePages();

if (app.Environment.IsDevelopment())
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.MapGet("/health", () => Results.Ok(new { service = "audit-service", status = "healthy" }));
app.MapGet("/", () => Results.Redirect("/swagger"));

app.MapPost("/audit/events", async (
    CreateAuditSnapshotRequest request,
    AppendAuditSnapshotUseCase useCase,
    CancellationToken cancellationToken) =>
{
    var validationError = request.Validate();
    if (validationError is not null)
    {
        return Results.ValidationProblem(validationError);
    }

    var snapshot = await useCase.ExecuteAsync(request.ToCommand(), cancellationToken);
    return Results.Created($"/audit/events/{snapshot.Id}", snapshot);
});

app.MapGet("/audit/orders/{correlationId:guid}", async (
    Guid correlationId,
    GetAuditTimelineUseCase useCase,
    CancellationToken cancellationToken) =>
{
    var snapshots = await useCase.ExecuteAsync(correlationId, cancellationToken);
    return Results.Ok(snapshots);
});

app.Run();
