import Bus from './eventBus'
let ws = new WebSocket("ws://localhost:8080/websocket");

// eslint-disable-next-line no-unused-vars
ws.onopen = function(e) {
    console.log(e)
};

ws.onmessage = function(e) {
// eslint-disable-next-line no-unused-vars
    try {
        let jsonObj = JSON.parse(e.data)
        if(jsonObj.topic){
            Bus.$emit(jsonObj.topic, jsonObj.message)
        }
    }catch (e) {
        console.log("这是其他消息")
    }
};

// eslint-disable-next-line no-unused-vars
ws.onclose = function(e) {

};
ws.onerror = function(e) {
    console.log(e)
}

export default ws