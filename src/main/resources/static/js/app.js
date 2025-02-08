const apiUrl = "/api/bots";
const strategiesUrl = "/api/strategy";
const profitConfigUrl = "/api/profit-config";

let strategies = [];
let profitConfigs = [];

async function loadOptions() {
    const strategySelect = document.getElementById("strategySelect");
    const profitConfigSelect = document.getElementById("profitConfigSelect");

    strategies = await fetch(strategiesUrl).then(res => res.json());
    profitConfigs = await fetch(profitConfigUrl).then(res => res.json());

    strategies.forEach(strategy => {
        const option = document.createElement("option");
        option.value = strategy.id;
        option.textContent = strategy.name;
        strategySelect.appendChild(option);
    });

    profitConfigs.forEach(config => {
        const option = document.createElement("option");
        option.value = config.id;
        option.textContent = `High: ${config.highProfit}, Low: ${config.lowProfit}`;
        profitConfigSelect.appendChild(option);
    });
}

async function loadBots() {
    const botsTable = document.getElementById("botsTable");
    botsTable.innerHTML = "";

    const bots = await fetch(apiUrl).then(res => res.json());

    bots.forEach(bot => {
        const row = document.createElement("tr");
        row.setAttribute("data-bot-id", bot.id);

        row.innerHTML = `
            <td>${bot.id}</td>
            <td>${bot.name}</td>
            <td>${bot.marketPair}</td>
            <td>${bot.deposit}</td>
            <td>${strategies.find(s => s.id === bot.strategyID)?.name || "Unknown"}</td>
            <td>${profitConfigs.find(p => p.id === bot.profitConfigID)?.highProfit || "Unknown"}</td>
            <td>${bot.futuresTakeProfitValue}</td>
            <td>${bot.futuresStopLoss}</td>
            <td>${bot.leverage}</td>
            <td>${bot.running ? "✅ On" : "❌ Off"}</td>
            <td>
                <button onclick="editBot(${bot.id})">✏️</button>
                <button onclick="deleteBot(${bot.id})">🗑️</button>
            </td>
        `;

        botsTable.appendChild(row);
    });
}

function editBot(botId) {
    const row = document.querySelector(`tr[data-bot-id="${botId}"]`);
    const cells = row.children;

    const name = cells[1].textContent;
    const market = cells[2].textContent;
    const deposit = cells[3].textContent;
    const currentStrategy = strategies.find(s => s.name === cells[4].textContent)?.id || "";
    const currentProfitConfig = profitConfigs.find(p => p.highProfit == cells[5].textContent)?.id || "";
    const futuresTakeProfitValue = cells[6].textContent;
    const futuresStopLoss = cells[7].textContent;
    const leverage = cells[8].textContent;
    const running = cells[9].textContent.includes("✅");

    row.innerHTML = `
        <td>${botId}</td>
        <td><input type="text" value="${name}" id="editName${botId}"></td>
        <td><input type="text" value="${market}" id="editMarket${botId}"></td>
        <td><input type="number" value="${deposit}" id="editDeposit${botId}"></td>
        <td>
            <select id="editStrategy${botId}">
                ${strategies.map(s => `<option value="${s.id}" ${s.id === currentStrategy ? "selected" : ""}>${s.name}</option>`).join("")}
            </select>
        </td>
        <td>
            <select id="editProfitConfig${botId}">
                ${profitConfigs.map(p => `<option value="${p.id}" ${p.id === currentProfitConfig ? "selected" : ""}>High: ${p.highProfit}, Low: ${p.lowProfit}</option>`).join("")}
            </select>
        </td>
        <td><input type="number" value="${futuresTakeProfitValue}" id="editFuturesTakeProfit${botId}"></td>
        <td><input type="number" value="${futuresStopLoss}" id="editFuturesStopLoss${botId}"></td>
        <td><input type="number" value="${leverage}" id="editLeverage${botId}"></td>
        <td>
            <input type="checkbox" id="editRunning${botId}" ${running ? "checked" : ""}>
        </td>
        <td>
            <button onclick="saveBot(${botId})">💾</button>
        </td>
    `;
}

async function saveBot(botId) {
    const name = document.getElementById(`editName${botId}`).value;
    const market = document.getElementById(`editMarket${botId}`).value;
    const deposit = parseFloat(document.getElementById(`editDeposit${botId}`).value);
    const strategyID = parseInt(document.getElementById(`editStrategy${botId}`).value);
    const profitConfigID = parseInt(document.getElementById(`editProfitConfig${botId}`).value);
    const futuresTakeProfitValue = parseFloat(document.getElementById(`editFuturesTakeProfit${botId}`).value);
    const futuresStopLoss = parseFloat(document.getElementById(`editFuturesStopLoss${botId}`).value);
    const leverage = parseInt(document.getElementById(`editLeverage${botId}`).value);
    const running = document.getElementById(`editRunning${botId}`).checked;

    const botData = { name, marketPair: market, deposit, strategyID, profitConfigID, futuresTakeProfitValue, futuresStopLoss, leverage, running };

    try {
        const response = await fetch(`${apiUrl}/${botId}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(botData)
        });

        if (!response.ok) throw new Error("Error bot update!");

        loadBots();
    } catch (error) {
        alert(error.message);
    }
}

async function deleteBot(botId) {
    if (confirm("Delete bot?")) {
        await fetch(`${apiUrl}/${botId}`, { method: "DELETE" });
        loadBots();
    }
}

document.getElementById("addBotForm").addEventListener("submit", async (e) => {
    e.preventDefault();

    const botData = {
        name: document.getElementById("botName").value,
        marketPair: document.getElementById("marketPair").value,
        deposit: parseFloat(document.getElementById("deposit").value),
        deadlineMinutes: parseFloat(document.getElementById("deadlineMinutes").value),
        takeProfitCheckValue: parseFloat(document.getElementById("takeProfitCheck").value),
        pullbackThreshold: parseFloat(document.getElementById("pullbackThreshold").value),
        maxTradeHours: parseFloat(document.getElementById("maxTradeHours").value),
        futuresTakeProfitValue: parseFloat(document.getElementById("fTakeProfit").value),
        futuresStopLoss: parseFloat(document.getElementById("fStopLoss").value),
        leverage: parseFloat(document.getElementById("fLeverage").value),
        strategyID: parseInt(document.getElementById("strategySelect").value),
        profitConfigID: parseInt(document.getElementById("profitConfigSelect").value),
        running: true
    };

    try {
        const response = await fetch(apiUrl, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(botData)
        });

        if (!response.ok) throw new Error("Error bot creation!");

        loadBots();
    } catch (error) {
        alert(error.message);
    }
});

loadOptions().then(loadBots);
