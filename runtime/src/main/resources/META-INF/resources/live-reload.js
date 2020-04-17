
const host = location.host.split(':')[0];
const port = location.host.split(':')[1];

const ws = new WebSocket(`ws://${host}:${port}/live-reload`)

// TODO: When websocket endpoint closes connection (e.g. live-reload of quarkus-backend), try to reconnect
ws.addEventListener('message', function (event) {
    console.log('live-reload-event from server', event);
    location.reload();
});

console.warn("live-reload activated")
