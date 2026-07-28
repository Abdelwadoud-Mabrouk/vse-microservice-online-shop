using System.Diagnostics;
using Microsoft.AspNetCore.Mvc;
using WarehouseService.Application;

namespace WarehouseService.Api.Endpoints;

public static class WarehouseEndpoints
{
    public static IEndpointRouteBuilder MapWarehouseEndpoints(this IEndpointRouteBuilder app)
    {
        app.MapGet("/inventory", (GetInventoryUseCase useCase) => Results.Ok(useCase.Execute()));
        app.MapGet("/inventory/{productId:guid}", GetInventoryItem);
        app.MapPost("/inventory", AddInventoryItemAsync);
        app.MapPut("/inventory/{productId:guid}", UpdateInventoryItemAsync);
        app.MapPatch("/inventory/{productId:guid}/stock", SetStockQuantityAsync);
        app.MapDelete("/inventory/{productId:guid}", RemoveInventoryItemAsync);

        app.MapPost("/reservations", ReserveAsync);
        app.MapDelete("/reservations/{reservationId:guid}", CancelAsync);
        app.MapPost("/reservations/{reservationId:guid}/commit", CommitAsync);

        return app;
    }

    private static IResult GetInventoryItem(Guid productId, IWarehouseRepository warehouse)
    {
        var item = warehouse.FindInventoryItem(productId);
        return item is null ? Results.NotFound() : Results.Ok(item);
    }

    private static async Task<IResult> AddInventoryItemAsync(
        SaveInventoryItemRequest request,
        AddInventoryItemUseCase useCase,
        HttpContext context,
        ILoggerFactory loggerFactory,
        CancellationToken cancellationToken)
    {
        var logger = loggerFactory.CreateLogger("Warehouse.Inventory");
        var stopwatch = Stopwatch.StartNew();
        var correlationId = context.GetCorrelationId();
        using var scope = BeginWarehouseScope(logger, correlationId, "warehouse.inventory.create", "/inventory", request.ProductId, null);

        logger.LogInformation(
            "Creating warehouse product {ProductId} with SKU {Sku}",
            request.ProductId,
            request.Sku);

        var validationError = request.Validate(requireProductId: true);
        if (validationError is not null)
        {
            LogValidationFailure(logger, "warehouse.inventory.create", "VALIDATION_FAILED", stopwatch.ElapsedMilliseconds);
            return Results.ValidationProblem(validationError);
        }

        var result = await useCase.ExecuteAsync(
            request.ToCommand(request.ProductId, correlationId),
            cancellationToken);

        if (!result.Success)
        {
            LogBusinessFailure(logger, "warehouse.inventory.create", "PRODUCT_CREATE_REJECTED", result.Message, stopwatch.ElapsedMilliseconds);
            return Results.Conflict(ToProblem("Product could not be created", result.Message, StatusCodes.Status409Conflict));
        }

        LogSuccess(logger, "warehouse.inventory.create", stopwatch.ElapsedMilliseconds);
        return Results.Created($"/inventory/{result.Item!.ProductId}", result.Item);
    }

    private static async Task<IResult> UpdateInventoryItemAsync(
        Guid productId,
        SaveInventoryItemRequest request,
        UpdateInventoryItemUseCase useCase,
        HttpContext context,
        ILoggerFactory loggerFactory,
        CancellationToken cancellationToken)
    {
        var logger = loggerFactory.CreateLogger("Warehouse.Inventory");
        var stopwatch = Stopwatch.StartNew();
        var correlationId = context.GetCorrelationId();
        using var scope = BeginWarehouseScope(logger, correlationId, "warehouse.inventory.update", "/inventory/{productId}", productId, null);

        var validationError = request.Validate(requireProductId: false);
        if (validationError is not null)
        {
            LogValidationFailure(logger, "warehouse.inventory.update", "VALIDATION_FAILED", stopwatch.ElapsedMilliseconds);
            return Results.ValidationProblem(validationError);
        }

        var result = await useCase.ExecuteAsync(
            request.ToCommand(productId, correlationId),
            cancellationToken);

        if (!result.Success)
        {
            var status = result.Message.Contains("SKU", StringComparison.OrdinalIgnoreCase)
                ? StatusCodes.Status409Conflict
                : StatusCodes.Status404NotFound;

            LogBusinessFailure(logger, "warehouse.inventory.update", status == StatusCodes.Status409Conflict ? "SKU_ALREADY_USED" : "PRODUCT_NOT_FOUND", result.Message, stopwatch.ElapsedMilliseconds);
            return status == StatusCodes.Status409Conflict
                ? Results.Conflict(ToProblem("Product could not be updated", result.Message, status))
                : Results.NotFound(ToProblem("Product could not be updated", result.Message, status));
        }

        LogSuccess(logger, "warehouse.inventory.update", stopwatch.ElapsedMilliseconds);
        return Results.Ok(result.Item);
    }

    private static async Task<IResult> SetStockQuantityAsync(
        Guid productId,
        SetStockQuantityRequest request,
        SetStockQuantityUseCase useCase,
        HttpContext context,
        ILoggerFactory loggerFactory,
        CancellationToken cancellationToken)
    {
        var logger = loggerFactory.CreateLogger("Warehouse.Inventory");
        var stopwatch = Stopwatch.StartNew();
        var correlationId = context.GetCorrelationId();
        using var scope = BeginWarehouseScope(logger, correlationId, "warehouse.inventory.set_stock", "/inventory/{productId}/stock", productId, null);

        var validationError = request.Validate();
        if (validationError is not null)
        {
            LogValidationFailure(logger, "warehouse.inventory.set_stock", "VALIDATION_FAILED", stopwatch.ElapsedMilliseconds);
            return Results.ValidationProblem(validationError);
        }

        var result = await useCase.ExecuteAsync(
            request.ToCommand(productId, correlationId),
            cancellationToken);

        if (!result.Success)
        {
            LogBusinessFailure(logger, "warehouse.inventory.set_stock", "PRODUCT_NOT_FOUND", result.Message, stopwatch.ElapsedMilliseconds);
            return Results.NotFound(ToProblem("Stock could not be changed", result.Message, StatusCodes.Status404NotFound));
        }

        LogSuccess(logger, "warehouse.inventory.set_stock", stopwatch.ElapsedMilliseconds);
        return Results.Ok(result.Item);
    }

    private static async Task<IResult> RemoveInventoryItemAsync(
        Guid productId,
        RemoveInventoryItemUseCase useCase,
        HttpContext context,
        ILoggerFactory loggerFactory,
        CancellationToken cancellationToken)
    {
        var logger = loggerFactory.CreateLogger("Warehouse.Inventory");
        var stopwatch = Stopwatch.StartNew();
        var correlationId = context.GetCorrelationId();
        using var scope = BeginWarehouseScope(logger, correlationId, "warehouse.inventory.delete", "/inventory/{productId}", productId, null);

        var result = await useCase.ExecuteAsync(productId, correlationId, cancellationToken);
        if (!result.Success)
        {
            var status = result.Message.Contains("does not exist", StringComparison.OrdinalIgnoreCase)
                ? StatusCodes.Status404NotFound
                : StatusCodes.Status409Conflict;

            LogBusinessFailure(logger, "warehouse.inventory.delete", status == StatusCodes.Status404NotFound ? "PRODUCT_NOT_FOUND" : "PRODUCT_HAS_OPEN_RESERVATION", result.Message, stopwatch.ElapsedMilliseconds);
            return status == StatusCodes.Status404NotFound
                ? Results.NotFound(ToProblem("Product could not be removed", result.Message, status))
                : Results.Conflict(ToProblem("Product could not be removed", result.Message, status));
        }

        LogSuccess(logger, "warehouse.inventory.delete", stopwatch.ElapsedMilliseconds);
        return Results.Ok(result.Item);
    }

    private static async Task<IResult> ReserveAsync(
        CreateReservationRequest request,
        ReserveStockUseCase useCase,
        IAuditSink audit,
        HttpContext context,
        ILoggerFactory loggerFactory,
        CancellationToken cancellationToken)
    {
        var correlationId = context.GetCorrelationId();
        var logger = loggerFactory.CreateLogger("Warehouse.Reservations");
        var stopwatch = Stopwatch.StartNew();
        using var scope = BeginWarehouseScope(logger, correlationId, "warehouse.reservation.reserve", "/reservations", request.OrderId, request.OrderId);

        logger.LogInformation(
            "Starting warehouse reservation for order {OrderId} with {ItemCount} item(s)",
            request.OrderId,
            request.Items?.Count ?? 0);

        var validationError = request.Validate();
        if (validationError is not null)
        {
            LogValidationFailure(logger, "warehouse.reservation.reserve", "VALIDATION_FAILED", stopwatch.ElapsedMilliseconds);
            await audit.RecordAsync(correlationId, "WAREHOUSE_RESERVATION_REJECTED", "FAILURE", request, cancellationToken);
            return Results.ValidationProblem(validationError);
        }

        var result = await useCase.ExecuteAsync(request.ToCommand(correlationId), cancellationToken);
        if (!result.Success)
        {
            LogBusinessFailure(logger, "warehouse.reservation.reserve", result.IsConflict ? "IDEMPOTENCY_CONFLICT" : "OUT_OF_STOCK", result.Message, stopwatch.ElapsedMilliseconds);
            return Results.Conflict(new ProblemDetails
            {
                Title = result.IsConflict ? "Idempotency conflict" : "Insufficient stock",
                Detail = result.Message,
                Status = StatusCodes.Status409Conflict
            });
        }

        LogSuccess(logger, "warehouse.reservation.reserve", stopwatch.ElapsedMilliseconds);
        return result.IsReplay
            ? Results.Ok(result.Reservation)
            : Results.Created($"/reservations/{result.Reservation!.ReservationId}", result.Reservation);
    }

    private static ProblemDetails ToProblem(string title, string detail, int status)
    {
        return new ProblemDetails
        {
            Title = title,
            Detail = detail,
            Status = status
        };
    }

    private static async Task<IResult> CancelAsync(
        Guid reservationId,
        CancelReservationUseCase useCase,
        HttpContext context,
        ILoggerFactory loggerFactory,
        CancellationToken cancellationToken)
    {
        var logger = loggerFactory.CreateLogger("Warehouse.Reservations");
        var stopwatch = Stopwatch.StartNew();
        var correlationId = context.GetCorrelationId();
        using var scope = BeginWarehouseScope(logger, correlationId, "warehouse.reservation.cancel", "/reservations/{reservationId}", reservationId, null);

        var result = await useCase.ExecuteAsync(reservationId, correlationId, cancellationToken);
        if (!result.Success)
        {
            var status = result.Outcome == ReservationTransitionOutcome.NotFound
                ? StatusCodes.Status404NotFound
                : StatusCodes.Status409Conflict;
            LogBusinessFailure(logger, "warehouse.reservation.cancel", status == 404 ? "RESERVATION_NOT_FOUND" : "INVALID_RESERVATION_STATE", result.Message, stopwatch.ElapsedMilliseconds);
            var problem = ToProblem("Reservation could not be cancelled", result.Message, status);
            return status == 404 ? Results.NotFound(problem) : Results.Conflict(problem);
        }

        LogSuccess(logger, "warehouse.reservation.cancel", stopwatch.ElapsedMilliseconds);
        return Results.Ok(result.Reservation);
    }

    private static async Task<IResult> CommitAsync(
        Guid reservationId,
        CommitReservationUseCase useCase,
        HttpContext context,
        ILoggerFactory loggerFactory,
        CancellationToken cancellationToken)
    {
        var logger = loggerFactory.CreateLogger("Warehouse.Reservations");
        var stopwatch = Stopwatch.StartNew();
        var correlationId = context.GetCorrelationId();
        using var scope = BeginWarehouseScope(logger, correlationId, "warehouse.reservation.commit", "/reservations/{reservationId}/commit", reservationId, null);

        ReservationTransitionResult result;
        try
        {
            result = await useCase.ExecuteAsync(reservationId, correlationId, cancellationToken);
        }
        catch (CommitFailureSimulationException exception)
        {
            logger.LogError(exception, "Controlled commit failure for reservation {ReservationId}", reservationId);
            return Results.Problem(
                title: "Warehouse commit failed",
                detail: exception.Message,
                statusCode: StatusCodes.Status500InternalServerError,
                extensions: new Dictionary<string, object?> { ["errorCode"] = "COMMIT_FAILURE_SIMULATED" });
        }

        if (!result.Success)
        {
            var status = result.Outcome == ReservationTransitionOutcome.NotFound
                ? StatusCodes.Status404NotFound
                : StatusCodes.Status409Conflict;
            LogBusinessFailure(logger, "warehouse.reservation.commit", status == 404 ? "RESERVATION_NOT_FOUND" : "INVALID_RESERVATION_STATE", result.Message, stopwatch.ElapsedMilliseconds);
            var problem = ToProblem("Reservation could not be committed", result.Message, status);
            return status == 404 ? Results.NotFound(problem) : Results.Conflict(problem);
        }

        LogSuccess(logger, "warehouse.reservation.commit", stopwatch.ElapsedMilliseconds);
        return Results.Ok(result.Reservation);
    }

    private static IDisposable? BeginWarehouseScope(
        ILogger logger,
        Guid correlationId,
        string operation,
        string route,
        Guid? entityId,
        Guid? orderId)
    {
        return logger.BeginScope(new Dictionary<string, object?>
        {
            ["service"] = "warehouse-service",
            ["correlationId"] = correlationId,
            ["operation"] = operation,
            ["route"] = route,
            ["entityId"] = entityId,
            ["orderId"] = orderId
        });
    }

    private static void LogSuccess(ILogger logger, string operation, long durationMs)
    {
        logger.LogInformation(
            "Operation {Operation} completed successfully with status {Status} in {DurationMs} ms",
            operation,
            "SUCCESS",
            durationMs);
    }

    private static void LogValidationFailure(ILogger logger, string operation, string errorCode, long durationMs)
    {
        logger.LogWarning(
            "Operation {Operation} failed validation with status {Status} and errorCode {ErrorCode} in {DurationMs} ms",
            operation,
            "VALIDATION_FAILED",
            errorCode,
            durationMs);
    }

    private static void LogBusinessFailure(ILogger logger, string operation, string errorCode, string reason, long durationMs)
    {
        logger.LogWarning(
            "Operation {Operation} failed with business status {Status}, errorCode {ErrorCode}, reason {Reason} in {DurationMs} ms",
            operation,
            "BUSINESS_FAILED",
            errorCode,
            reason,
            durationMs);
    }
}
