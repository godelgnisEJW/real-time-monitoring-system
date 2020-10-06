<template>
    <div>
        <Row type="flex" :gutter="16">
            <Col span="12">
                <interface-line-box/>
            </Col>
            <Col span="12">
                <queue-line-box/>
            </Col>
        </Row>
        <Row type="flex" :gutter="16">
            <Col span="12">
                <message-line-box/>
            </Col>
            <Col span="12">
                <file-line-box/>
            </Col>
        </Row>
    </div>
</template>

<script>
    import InterfaceLineBox from '../base/InterfaceLineBox'
    import MessageLineBox from "../base/MessageLineBox"
    import FileLineBox from "../base/FileLineBox";
    import QueueLineBox from "../base/QueueLineBox";
    export default {
        components:{
            InterfaceLineBox,
            MessageLineBox,
            FileLineBox,
            QueueLineBox
        }
    }
</script>

<style scoped>
    .demo-spin-icon-load{
        animation: ani-demo-spin 1s linear infinite;
    }
    @keyframes ani-demo-spin {
        from { transform: rotate(0deg);}
        50%  { transform: rotate(180deg);}
        to   { transform: rotate(360deg);}
    }
    .demo-spin-col{
        height: 100px;
        position: relative;
        border: 1px solid #eee;
    }
</style>