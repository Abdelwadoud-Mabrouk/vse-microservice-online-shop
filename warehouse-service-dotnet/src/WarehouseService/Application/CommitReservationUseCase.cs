using Microsoft.Extensions.Logging.Abstractions;

namespace WarehouseService.Application;

public sealed class CommitReservationUseCase(
    IWarehouseRepository warehouse,
    IAuditSink audit,
    ILogger<CommitReservationUseCase>? logger = null,
    IConfiguration? configuration = null,
    IHostEnvironment? environment = null)
{
    private readonly ILogger<CommitReservationUseCase> _logger = logger ?? NullLogger<CommitReservationUseCase>.Instance;

    public async Task<ReservationTransitionResult> ExecuteAsync(Guid reservationId, Guid correlationId, CancellationToken cancellationToken)
    {
        using var scope = _logger.BeginScope(new Dictionary<string, object?>
        {
            ["service"] = "warehouse-service",
            ["correlationId"] = correlationId,
            ["operation"] = "warehouse.commit_reservation",
            ["entityId"] = reservationId
        });

        if (environment?.IsDevelopment() == true
            && configuration?.GetValue<bool>("FailureSimulation:FailCommit") == true)
        {
            _logger.LogError(
                "Commit failure simulated for reservation {ReservationId} with status {Status} and errorCode {ErrorCode}",
                reservationId,
                "FAILURE",
                "COMMIT_FAILURE_SIMULATED");
            throw new CommitFailureSimulationException();
        }

        var result = warehouse.Commit(reservationId);
        if (!result.Success)
        {
            _logger.LogWarning(
                "Reservation {ReservationId} could not be committed with outcome {Outcome}, status {Status} and errorCode {ErrorCode}",
                reservationId,
                result.Outcome,
                "BUSINESS_FAILED",
                result.Outcome == ReservationTransitionOutcome.NotFound ? "RESERVATION_NOT_FOUND" : "INVALID_RESERVATION_STATE");
            return result;
        }

        _logger.LogInformation(
            "Reservation {ReservationId} committed with status {Status}, idempotentReplay {IdempotentReplay}",
            reservationId,
            "SUCCESS",
            result.Outcome == ReservationTransitionOutcome.AlreadyApplied);

        if (result.Outcome == ReservationTransitionOutcome.Applied)
        {
            await audit.RecordAsync(correlationId, "WAREHOUSE_COMMITTED", "SUCCESS", result.Reservation!, cancellationToken);
        }

        return result;
    }
}

public sealed class CommitFailureSimulationException()
    : Exception("Warehouse commit failure was simulated for an integration test.");
