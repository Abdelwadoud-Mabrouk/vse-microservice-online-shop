using System.Globalization;
using System.Text;
using InvoiceService.Application;
using InvoiceService.Domain;

namespace InvoiceService.Infrastructure;

public sealed class PdfInvoiceDocumentWriter : IInvoiceDocumentWriter
{
    private readonly string _pdfDirectory;
    private readonly bool _forceFailure;

    public PdfInvoiceDocumentWriter(IConfiguration configuration, IWebHostEnvironment environment)
    {
        var invoiceDirectory = configuration["INVOICE_DATA_DIR"]
            ?? Path.Combine(environment.ContentRootPath, "data", "invoices");

        _pdfDirectory = Path.Combine(invoiceDirectory, "pdf");
        Directory.CreateDirectory(_pdfDirectory);

        _forceFailure = bool.TryParse(configuration["INVOICE_FORCE_FAILURE"], out var forceFailure) && forceFailure;
    }

    public async Task<string> WriteAsync(Invoice invoice, CancellationToken cancellationToken)
    {
        if (_forceFailure)
        {
            throw new InvalidOperationException("Invoice generation is forced to fail by configuration.");
        }

        var path = Path.Combine(_pdfDirectory, $"invoice-{invoice.InvoiceId}.pdf");
        var bytes = CreatePdf(invoice);
        await File.WriteAllBytesAsync(path, bytes, cancellationToken);
        return path;
    }

    private static byte[] CreatePdf(Invoice invoice)
    {
        var content = new StringBuilder();

        content.AppendLine("0.96 0.98 1 rg");
        content.AppendLine("0 742 595 100 re f");
        content.AppendLine("0.12 0.32 0.61 rg");
        content.AppendLine("0 738 595 4 re f");
        content.AppendLine("0 g");

        AddText(content, 50, 810, 16, "Online-Shop GmbH", bold: true);
        AddText(content, 50, 792, 8, "Inhaber: Abdelwadoud Mabrouk & Mohamed Amine Grioui");
        AddText(content, 50, 776, 8, "VSE Online-Shop GmbH");

        AddText(content, 545, 810, 25, "RECHNUNG", bold: true, alignRight: true);

        AddText(content, 50, 718, 8, $"Rechnungs-ID: {invoice.InvoiceId}");
        AddText(content, 50, 704, 8, $"Bestell-ID: {invoice.OrderId}");

        content.AppendLine("0.985 0.985 0.985 rg");
        content.AppendLine("50 604 230 76 re f");
        content.AppendLine("315 556 230 124 re f");
        content.AppendLine("0 g");

        AddText(content, 62, 662, 10, "Rechnung an", bold: true);
        AddText(content, 62, 642, 9, invoice.Customer?.Name ?? invoice.CustomerEmail);
        AddText(content, 62, 626, 9, invoice.Customer?.Street ?? "-");
        AddText(content, 62, 610, 9, $"{invoice.Customer?.PostalCode ?? ""} {invoice.Customer?.City ?? ""}".Trim());

        AddText(content, 327, 662, 10, "Zahlungs- und Kundendaten", bold: true);
        AddText(content, 327, 642, 8, "Transaktion", bold: true);
        AddText(content, 405, 642, 8, Trim(invoice.TransactionId, 26));
        AddText(content, 327, 626, 8, "E-Mail", bold: true);
        AddText(content, 405, 626, 8, Trim(invoice.CustomerEmail, 28));
        AddText(content, 327, 610, 8, "Land", bold: true);
        AddText(content, 405, 610, 8, invoice.Customer?.Country ?? "-");
        AddText(content, 327, 594, 8, "Datum", bold: true);
        AddText(content, 405, 594, 8, invoice.CreatedAt.ToString("dd.MM.yyyy", CultureInfo.InvariantCulture));
        AddText(content, 327, 578, 8, "Waehrung", bold: true);
        AddText(content, 405, 578, 8, invoice.Currency);

        content.AppendLine("0.12 0.32 0.61 rg");
        content.AppendLine("50 526 495 24 re f");
        content.AppendLine("1 g");
        AddText(content, 58, 534, 8, "SKU", bold: true);
        AddText(content, 178, 534, 8, "Artikel", bold: true);
        AddText(content, 355, 534, 8, "Menge", bold: true, alignRight: true);
        AddText(content, 465, 534, 8, "Einzelpreis", bold: true, alignRight: true);
        AddText(content, 535, 534, 8, "Summe", bold: true, alignRight: true);
        content.AppendLine("0 g");

        var y = 501;
        foreach (var item in invoice.Items)
        {
            var total = item.Quantity * item.UnitPrice;
            AddText(content, 58, y, 8, Trim(item.Sku, 18));
            AddText(content, 178, y, 8, Trim(item.Name, 30));
            AddText(content, 355, y, 8, item.Quantity.ToString(CultureInfo.InvariantCulture), alignRight: true);
            AddText(content, 465, y, 8, Money(item.UnitPrice, invoice.Currency), alignRight: true);
            AddText(content, 535, y, 8, Money(total, invoice.Currency), alignRight: true);
            content.AppendLine("0.86 0.88 0.90 RG");
            content.AppendLine($"50 {y - 9} m 545 {y - 9} l S");
            y -= 24;
        }

        content.AppendLine("0 g");
        y -= 16;
        content.AppendLine("0.12 0.32 0.61 RG");
        content.AppendLine($"335 {y + 14} m 545 {y + 14} l S");
        AddText(content, 365, y, 11, "Gesamtbetrag", bold: true);
        AddText(content, 535, y, 11, Money(invoice.Amount, invoice.Currency), bold: true, alignRight: true);
        content.AppendLine("0 g");

        content.AppendLine("0.86 0.88 0.90 RG");
        content.AppendLine("50 102 m 545 102 l S");
        content.AppendLine("0 g");
        AddText(content, 50, 84, 7, "PDF dauerhaft gespeichert. Zugriff ueber Rechnungs-ID oder Kunden-E-Mail.");

        return BuildPdfDocument(content.ToString());
    }

    private static byte[] BuildPdfDocument(string pageContent)
    {
        var objects = new List<string>
        {
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R /F2 5 0 R >> >> /Contents 6 0 R >>",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>",
            $"<< /Length {Encoding.ASCII.GetByteCount(pageContent)} >>\nstream\n{pageContent}\nendstream"
        };

        var pdf = new StringBuilder();
        var offsets = new List<int> { 0 };

        pdf.Append("%PDF-1.4\n");
        pdf.Append("%\u00e2\u00e3\u00cf\u00d3\n");

        for (var i = 0; i < objects.Count; i++)
        {
            offsets.Add(Encoding.ASCII.GetByteCount(pdf.ToString()));
            pdf.Append($"{i + 1} 0 obj\n");
            pdf.Append(objects[i]);
            pdf.Append("\nendobj\n");
        }

        var xrefOffset = Encoding.ASCII.GetByteCount(pdf.ToString());
        pdf.Append("xref\n");
        pdf.Append($"0 {objects.Count + 1}\n");
        pdf.Append("0000000000 65535 f \n");

        foreach (var offset in offsets.Skip(1))
        {
            pdf.Append($"{offset:0000000000} 00000 n \n");
        }

        pdf.Append("trailer\n");
        pdf.Append($"<< /Size {objects.Count + 1} /Root 1 0 R >>\n");
        pdf.Append("startxref\n");
        pdf.Append(xrefOffset);
        pdf.Append("\n%%EOF");

        return Encoding.ASCII.GetBytes(pdf.ToString());
    }

    private static string PdfText(string value)
    {
        return value
            .Replace("\\", "\\\\", StringComparison.Ordinal)
            .Replace("(", "\\(", StringComparison.Ordinal)
            .Replace(")", "\\)", StringComparison.Ordinal)
            .Replace("€", "EUR", StringComparison.Ordinal);
    }

    private static void AddText(StringBuilder content, int x, int y, int size, string value, bool bold = false, bool alignRight = false)
    {
        var font = bold ? "F2" : "F1";
        var escaped = PdfText(value);
        var adjustedX = alignRight ? x - EstimateWidth(value, size) : x;

        content.AppendLine("BT");
        content.AppendLine($"/{font} {size} Tf");
        content.AppendLine($"{adjustedX} {y} Td");
        content.AppendLine($"({escaped}) Tj");
        content.AppendLine("ET");
    }

    private static int EstimateWidth(string value, int size)
    {
        return (int)Math.Round(value.Length * size * 0.52);
    }

    private static string Money(decimal value, string currency)
    {
        var culture = CultureInfo.GetCultureInfo("de-DE");
        return $"{value.ToString("N2", culture)} {currency}";
    }

    private static string Trim(string value, int maxLength)
    {
        return value.Length <= maxLength ? value : value[..(maxLength - 3)] + "...";
    }
}
