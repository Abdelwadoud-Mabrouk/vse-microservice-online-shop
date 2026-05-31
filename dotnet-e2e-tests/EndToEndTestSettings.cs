public sealed record EndToEndTestSettings(
    Uri AuditUrl,
    Uri WarehouseUrl,
    Uri InvoiceUrl,
    string KafkaBootstrapServers,
    string PaymentSucceededTopic,
    string AuditEventsTopic)
{
    public static EndToEndTestSettings FromEnvironment()
    {
        return new EndToEndTestSettings(
            ReadUri("E2E_AUDIT_URL", "http://localhost:5081"),
            ReadUri("E2E_WAREHOUSE_URL", "http://localhost:5082"),
            ReadUri("E2E_INVOICE_URL", "http://localhost:5083"),
            Environment.GetEnvironmentVariable("E2E_KAFKA_BOOTSTRAP_SERVERS") ?? "localhost:9092",
            Environment.GetEnvironmentVariable("E2E_PAYMENT_SUCCEEDED_TOPIC") ?? "payment-succeeded",
            Environment.GetEnvironmentVariable("E2E_AUDIT_EVENTS_TOPIC") ?? "audit-events");
    }

    private static Uri ReadUri(string name, string fallback)
    {
        return new Uri(Environment.GetEnvironmentVariable(name) ?? fallback);
    }
}
