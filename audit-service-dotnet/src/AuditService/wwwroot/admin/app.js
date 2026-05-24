const ordersElement = document.querySelector("#orders");
const timelineElement = document.querySelector("#timeline");
const selectedCorrelationElement = document.querySelector("#selectedCorrelation");
const statusFilterElement = document.querySelector("#statusFilter");
const correlationFilterElement = document.querySelector("#correlationFilter");
const manualCorrelationIdElement = document.querySelector("#manualCorrelationId");
const refreshButton = document.querySelector("#refreshButton");
const loadTimelineButton = document.querySelector("#loadTimelineButton");
const copyCorrelationButton = document.querySelector("#copyCorrelationButton");
const serviceHealthElement = document.querySelector("#serviceHealth");
const healthUpdatedElement = document.querySelector("#healthUpdated");
const lastRefreshElement = document.querySelector("#lastRefresh");
const orderCountElement = document.querySelector("#orderCount");
const failureCountElement = document.querySelector("#failureCount");
const healthyCountElement = document.querySelector("#healthyCount");
const reloadInventoryButton = document.querySelector("#reloadInventoryButton");
const resetInventoryFormButton = document.querySelector("#resetInventoryFormButton");
const inventorySummaryElement = document.querySelector("#inventorySummary");
const inventorySearchElement = document.querySelector("#inventorySearch");
const inventoryCategoryFilterElement = document.querySelector("#inventoryCategoryFilter");
const inventoryPageSizeElement = document.querySelector("#inventoryPageSize");
const previousInventoryPageButton = document.querySelector("#previousInventoryPageButton");
const nextInventoryPageButton = document.querySelector("#nextInventoryPageButton");
const inventoryPageInfoElement = document.querySelector("#inventoryPageInfo");
const inventoryForm = document.querySelector("#inventoryForm");
const inventoryMessageElement = document.querySelector("#inventoryMessage");
const inventoryItemsElement = document.querySelector("#inventoryItems");
const inventoryTemplate = document.querySelector("#inventoryTemplate");
const orderTemplate = document.querySelector("#orderTemplate");
const timelineTemplate = document.querySelector("#timelineTemplate");

let orders = [];
let activeCorrelationId = null;
let inventory = [];
let inventoryPage = 1;

const config = window.adminConfig ?? {};
const auditBaseUrl = resolveAuditBaseUrl(config.auditBaseUrl);
const invoiceServiceBaseUrl = resolveServiceBaseUrl(config.invoiceBaseUrl, "5083");

async function fetchJson(url) {
  const response = await fetch(`${auditBaseUrl}${url}`, { headers: { Accept: "application/json" } });
  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }

  return response.json();
}

async function sendJson(url, method, body) {
  const response = await fetch(`${auditBaseUrl}${url}`, {
    method,
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json"
    },
    body: body ? JSON.stringify(body) : undefined
  });

  const text = await response.text();
  const payload = text ? JSON.parse(text) : null;
  if (!response.ok) {
    throw new Error(payload?.detail || payload?.title || `Request failed: ${response.status}`);
  }

  return payload;
}

function resolveAuditBaseUrl(value) {
  if (value) {
    return value.replace(/\/$/, "");
  }

  if (window.location.protocol === "file:") {
    return "http://localhost:5081";
  }

  return "";
}

function resolveServiceBaseUrl(value, fallbackPort) {
  if (value) {
    return value.replace(/\/$/, "");
  }

  const isLocal = ["localhost", "127.0.0.1", ""].includes(window.location.hostname);
  if (window.location.protocol === "file:" || isLocal) {
    return `http://localhost:${fallbackPort}`;
  }

  return window.location.origin;
}

function statusClass(status) {
  return String(status || "").toLowerCase();
}

function formatDate(value) {
  if (!value) {
    return "-";
  }

  return new Intl.DateTimeFormat("de-DE", {
    dateStyle: "short",
    timeStyle: "medium"
  }).format(new Date(value));
}

function setBadge(element, status) {
  element.textContent = status || "UNKNOWN";
  element.className = `badge ${statusClass(status)}`;
}

function businessStatus(order) {
  const eventType = String(order.latestEventType || "");
  const statusCode = String(order.latestStatusCode || "");

  if (statusCode === "FAILURE") {
    return "Fehler pruefen";
  }

  if (statusCode === "COMPENSATED" || eventType.includes("ROLLBACK") || eventType.includes("REFUNDED")) {
    return "Rollback abgeschlossen";
  }

  if (eventType.includes("INVOICE_CREATED")) {
    return "Rechnung erstellt";
  }

  if (eventType.includes("WAREHOUSE_COMMITTED") || eventType.includes("ORDER_COMPLETED")) {
    return "Abgeschlossen";
  }

  if (eventType.includes("WAREHOUSE_RESERVED")) {
    return "Bestand reserviert";
  }

  return "In Bearbeitung";
}

function buildEventLinks(event) {
  const links = [];
  const invoiceId = findInvoiceId(event.payload);

  if (invoiceId) {
    links.push({
      label: "Rechnung",
      href: `${invoiceServiceBaseUrl}/invoices/${invoiceId}/pdf`,
      download: `rechnung-${invoiceId}.pdf`
    });
  }

  return links;
}

function findInvoiceId(payload) {
  if (!payload || typeof payload !== "object") {
    return null;
  }

  return payload.invoiceId
    || payload.InvoiceId
    || payload.invoice?.invoiceId
    || payload.Invoice?.InvoiceId
    || null;
}

function renderOrders() {
  const status = statusFilterElement.value;
  const search = correlationFilterElement.value.trim().toLowerCase();

  const visibleOrders = orders.filter(order => {
    const matchesStatus = !status || order.latestStatusCode === status;
    const matchesSearch = !search || order.correlationId.toLowerCase().includes(search);
    return matchesStatus && matchesSearch;
  });

  ordersElement.replaceChildren();

  if (visibleOrders.length === 0) {
    const empty = document.createElement("div");
    empty.className = "empty";
    empty.textContent = "Keine Bestellungen gefunden.";
    ordersElement.append(empty);
    return;
  }

  for (const order of visibleOrders) {
    const node = orderTemplate.content.firstElementChild.cloneNode(true);
    node.querySelector('[data-field="latestEventType"]').textContent = order.latestEventType;
    node.querySelector('[data-field="correlationId"]').textContent = order.correlationId;
    node.querySelector('[data-field="businessStatus"]').textContent = businessStatus(order);
    setBadge(node.querySelector('[data-field="latestStatusCode"]'), order.latestStatusCode);

    if (order.correlationId === activeCorrelationId) {
      node.classList.add("is-active");
    }

    node.addEventListener("click", () => loadTimeline(order.correlationId));
    ordersElement.append(node);
  }
}

function updateOrderStats() {
  orderCountElement.textContent = String(orders.length);
  failureCountElement.textContent = String(orders.filter(order =>
    order.latestStatusCode === "FAILURE" || order.latestStatusCode === "COMPENSATED"
  ).length);
}

function renderTimeline(events) {
  timelineElement.replaceChildren();

  if (events.length === 0) {
    const empty = document.createElement("div");
    empty.className = "empty";
    empty.textContent = "Keine Audit-Events für diese correlationId gefunden.";
    timelineElement.append(empty);
    return;
  }

  for (const event of events) {
    const node = timelineTemplate.content.firstElementChild.cloneNode(true);
    node.querySelector('[data-field="eventType"]').textContent = event.eventType;
    setBadge(node.querySelector('[data-field="statusCode"]'), event.statusCode);
    node.querySelector('[data-field="meta"]').textContent =
      `${event.service} · ${event.actor} · ${formatDate(event.timestamp)}`;
    const linksElement = node.querySelector('[data-field="links"]');
    const links = buildEventLinks(event);
    for (const link of links) {
      const anchor = document.createElement("a");
      anchor.href = link.href;
      anchor.target = "_blank";
      anchor.rel = "noreferrer";
      anchor.textContent = link.label;
      if (link.download) {
        anchor.download = link.download;
      }
      linksElement.append(anchor);
    }

    if (links.length === 0) {
      linksElement.remove();
    }

    node.querySelector('[data-field="payload"]').textContent =
      JSON.stringify(event.payload ?? {}, null, 2);
    timelineElement.append(node);
  }
}

async function loadOrders() {
  ordersElement.innerHTML = '<div class="empty">Bestellungen werden geladen...</div>';
  try {
    orders = await fetchJson("/audit/orders");
    renderOrders();
    updateOrderStats();
    lastRefreshElement.textContent = `Aktualisiert: ${formatDate(new Date().toISOString())}`;
  } catch (error) {
    ordersElement.innerHTML = `<div class="empty">Bestellungen konnten nicht geladen werden: ${error.message}</div>`;
  }
}

async function loadServiceHealth() {
  try {
    const services = await fetchJson("/admin/service-health");
    serviceHealthElement.replaceChildren();

    for (const service of services) {
      const item = document.createElement("div");
      item.className = "health-item";

      const name = document.createElement("span");
      name.textContent = service.service;

      const status = document.createElement("span");
      status.className = "health-status";
      const dot = document.createElement("span");
      dot.className = `health-dot ${statusClass(service.status)}`;
      status.append(dot, ` ${service.status}`);

      item.append(name, status);
      serviceHealthElement.append(item);
    }

    const healthy = services.filter(service => service.status === "healthy").length;
    healthyCountElement.textContent = `${healthy} / ${services.length}`;
    healthUpdatedElement.textContent = `aktualisiert: ${formatDate(new Date().toISOString())}`;
  } catch (error) {
    serviceHealthElement.innerHTML = `<div class="empty">Service-Zustand konnte nicht geladen werden: ${error.message}</div>`;
    healthUpdatedElement.textContent = "Fehler";
  }
}

function renderInventory() {
  inventoryItemsElement.replaceChildren();
  renderInventoryCategoryOptions();

  const filteredItems = getFilteredInventory();
  const pageSize = Number(inventoryPageSizeElement.value);
  const totalPages = Math.max(1, Math.ceil(filteredItems.length / pageSize));
  inventoryPage = Math.min(inventoryPage, totalPages);
  const pageStart = (inventoryPage - 1) * pageSize;
  const visibleItems = filteredItems.slice(pageStart, pageStart + pageSize);

  inventorySummaryElement.textContent = `${filteredItems.length} von ${inventory.length} Produkten`;
  inventoryPageInfoElement.textContent = `Seite ${inventoryPage} / ${totalPages}`;
  previousInventoryPageButton.disabled = inventoryPage <= 1;
  nextInventoryPageButton.disabled = inventoryPage >= totalPages;

  if (filteredItems.length === 0) {
    const empty = document.createElement("div");
    empty.className = "empty";
    empty.textContent = "Keine passenden Produkte gefunden.";
    inventoryItemsElement.append(empty);
    return;
  }

  for (const item of visibleItems) {
    const node = inventoryTemplate.content.firstElementChild.cloneNode(true);
    node.querySelector('[data-field="name"]').textContent = item.name;
    node.querySelector('[data-field="meta"]').textContent =
      `${item.sku} · ${item.category}`;
    node.querySelector('[data-field="productId"]').textContent = item.productId;

    const quantityInput = node.querySelector('[data-field="quantity"]');
    quantityInput.value = item.availableQuantity;

    node.querySelector('[data-action="stock"]').addEventListener("click", () =>
      setInventoryQuantity(item.productId, Number(quantityInput.value)));
    node.querySelector('[data-action="edit"]').addEventListener("click", () =>
      fillInventoryForm(item));
    node.querySelector('[data-action="delete"]').addEventListener("click", () =>
      deleteInventoryItem(item.productId));

    inventoryItemsElement.append(node);
  }
}

function getFilteredInventory() {
  const search = inventorySearchElement.value.trim().toLowerCase();
  const category = inventoryCategoryFilterElement.value;

  return inventory
    .filter(item => !category || item.category === category)
    .filter(item => {
      if (!search) {
        return true;
      }

      return item.name.toLowerCase().includes(search)
        || item.sku.toLowerCase().includes(search)
        || item.productId.toLowerCase().includes(search)
        || item.category.toLowerCase().includes(search);
    })
    .sort((left, right) => left.name.localeCompare(right.name, "de"));
}

function renderInventoryCategoryOptions() {
  const selected = inventoryCategoryFilterElement.value;
  const categories = [...new Set(inventory.map(item => item.category).filter(Boolean))]
    .sort((left, right) => left.localeCompare(right, "de"));

  inventoryCategoryFilterElement.replaceChildren(new Option("Alle Kategorien", ""));
  for (const category of categories) {
    inventoryCategoryFilterElement.append(new Option(category, category));
  }

  inventoryCategoryFilterElement.value = categories.includes(selected) ? selected : "";
}

async function loadInventory() {
  inventoryItemsElement.innerHTML = '<div class="empty">Bestand wird geladen...</div>';
  try {
    inventory = await fetchJson("/admin/warehouse/inventory");
    inventoryPage = 1;
    renderInventory();
  } catch (error) {
    inventoryItemsElement.innerHTML = `<div class="empty">Bestand konnte nicht geladen werden: ${error.message}</div>`;
  }
}

function fillInventoryForm(item) {
  document.querySelector("#productId").value = item.productId;
  document.querySelector("#sku").value = item.sku;
  document.querySelector("#productName").value = item.name;
  document.querySelector("#category").value = item.category;
  document.querySelector("#availableQuantity").value = item.availableQuantity;
  inventoryMessageElement.textContent = "Produkt ist zum Bearbeiten geladen.";
  document.querySelector("#sku").focus();
}

function readInventoryForm() {
  const productId = document.querySelector("#productId").value.trim() || crypto.randomUUID();
  return {
    productId,
    sku: document.querySelector("#sku").value.trim(),
    name: document.querySelector("#productName").value.trim(),
    category: document.querySelector("#category").value.trim(),
    availableQuantity: Number(document.querySelector("#availableQuantity").value)
  };
}

function validateInventoryRequest(request) {
  if (!request.sku || !request.name || !request.category || Number.isNaN(request.availableQuantity)) {
    return "SKU, Name, Kategorie und Bestand sind erforderlich.";
  }

  if (request.availableQuantity < 0) {
    return "Bestand darf nicht negativ sein.";
  }

  return null;
}

async function saveInventoryItem(event) {
  event.preventDefault();
  const request = readInventoryForm();
  const validationError = validateInventoryRequest(request);
  if (validationError) {
    inventoryMessageElement.textContent = validationError;
    return;
  }

  const exists = inventory.some(item => item.productId === request.productId);
  try {
    await sendJson(
      exists ? `/admin/warehouse/inventory/${request.productId}` : "/admin/warehouse/inventory",
      exists ? "PUT" : "POST",
      request);
    inventoryMessageElement.textContent = exists ? "Produkt wurde aktualisiert." : "Produkt wurde angelegt.";
    inventoryForm.reset();
    inventoryPage = 1;
    await loadInventory();
  } catch (error) {
    inventoryMessageElement.textContent = `Speichern fehlgeschlagen: ${error.message}`;
  }
}

async function setInventoryQuantity(productId, availableQuantity) {
  if (Number.isNaN(availableQuantity) || availableQuantity < 0) {
    inventoryMessageElement.textContent = "Bestand muss eine Zahl ab 0 sein.";
    return;
  }

  try {
    await sendJson(`/admin/warehouse/inventory/${productId}/stock`, "PATCH", { availableQuantity });
    inventoryMessageElement.textContent = "Bestand wurde aktualisiert.";
    await loadInventory();
  } catch (error) {
    inventoryMessageElement.textContent = `Bestand konnte nicht geändert werden: ${error.message}`;
  }
}

async function deleteInventoryItem(productId) {
  try {
    await sendJson(`/admin/warehouse/inventory/${productId}`, "DELETE");
    inventoryMessageElement.textContent = "Produkt wurde gelöscht.";
    await loadInventory();
  } catch (error) {
    inventoryMessageElement.textContent = `Löschen fehlgeschlagen: ${error.message}`;
  }
}

async function loadTimeline(correlationId) {
  const id = correlationId.trim();
  if (!id) {
    return;
  }

  activeCorrelationId = id;
  manualCorrelationIdElement.value = id;
  selectedCorrelationElement.textContent = id;
  copyCorrelationButton.disabled = false;
  renderOrders();
  timelineElement.innerHTML = '<div class="empty">Timeline wird geladen...</div>';

  try {
    const events = await fetchJson(`/audit/orders/${encodeURIComponent(id)}`);
    renderTimeline(events);
  } catch (error) {
    timelineElement.innerHTML = `<div class="empty">Timeline konnte nicht geladen werden: ${error.message}</div>`;
  }
}

refreshButton.addEventListener("click", () => {
  loadOrders();
  loadServiceHealth();
  loadInventory();
});
statusFilterElement.addEventListener("change", renderOrders);
correlationFilterElement.addEventListener("input", renderOrders);
loadTimelineButton.addEventListener("click", () => loadTimeline(manualCorrelationIdElement.value));
reloadInventoryButton.addEventListener("click", loadInventory);
resetInventoryFormButton.addEventListener("click", () => {
  inventoryForm.reset();
  inventoryMessageElement.textContent = "Neues Produkt kann erfasst werden.";
  document.querySelector("#sku").focus();
});
inventorySearchElement.addEventListener("input", () => {
  inventoryPage = 1;
  renderInventory();
});
inventoryCategoryFilterElement.addEventListener("change", () => {
  inventoryPage = 1;
  renderInventory();
});
inventoryPageSizeElement.addEventListener("change", () => {
  inventoryPage = 1;
  renderInventory();
});
previousInventoryPageButton.addEventListener("click", () => {
  inventoryPage = Math.max(1, inventoryPage - 1);
  renderInventory();
});
nextInventoryPageButton.addEventListener("click", () => {
  inventoryPage += 1;
  renderInventory();
});
inventoryForm.addEventListener("submit", saveInventoryItem);
copyCorrelationButton.addEventListener("click", async () => {
  if (!activeCorrelationId) {
    return;
  }

  await navigator.clipboard.writeText(activeCorrelationId);
  copyCorrelationButton.textContent = "Kopiert";
  setTimeout(() => {
    copyCorrelationButton.textContent = "Kopieren";
  }, 1200);
});
manualCorrelationIdElement.addEventListener("keydown", event => {
  if (event.key === "Enter") {
    loadTimeline(manualCorrelationIdElement.value);
  }
});

loadOrders();
loadServiceHealth();
loadInventory();
setInterval(loadServiceHealth, 10000);
