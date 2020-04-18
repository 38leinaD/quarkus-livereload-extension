const RECONNECT_BACKOFFS = [500, 1000, 1000, 1000, 1000, 1000, 2000, 2000, 2000, 5000];
let currentBackoffIndex = 0;

const host = location.host.split(':')[0];
const port = location.host.split(':')[1];

let ws = null;

function connectToLiveReload() {

    try {
        ws = new WebSocket(`ws://${host}:${port}/live-reload`)
    }
    catch (e) {
        console.debug("error while connecting to websocket endpoint", e)
        scheduleReconnect();
        return;
    }
    ws.onmessage = (event) => {
        console.log('live-reload triggerd by server', event);
        location.reload();
    };

    ws.onopen = (event) => {
        console.warn("live-reload activated")
        currentBackoffIndex = 0;
    };

    ws.onclose = (event) => {
        console.debug("live-reload websocket closed", event)
        scheduleReconnect();
    };

    ws.onerror = (event) => {
        console.debug("live-reload websocket error", event)
    };
}

function scheduleReconnect() {
    console.debug("Reconnect scheduled")
    setTimeout(() => {
        connectToLiveReload();
    }, RECONNECT_BACKOFFS[currentBackoffIndex])

    currentBackoffIndex = Math.min(currentBackoffIndex + 1, RECONNECT_BACKOFFS.length - 1) 
}

connectToLiveReload();

