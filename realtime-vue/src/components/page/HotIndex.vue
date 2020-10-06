<template>
    <div>
        <Row>
            <online-num/>
        </Row>
        <Row>
            <Col span="24">
                <map-box/>
            </Col>
        </Row>
        <Row type="flex" :gutter="16">
            <Col span="12">
                <line-box/>
            </Col>
            <Col span="10">
                <range-bar-box/>
            </Col>
        </Row>
    </div>
</template>

<script>
    import OnlineNum from "../base/OnlineNum";
    import LineBox from "../base/LineBox"
    import MapBox from "../base/MapBox"
    import RangeBarBox from "../base/RangeBarBox";
    export  default {
        components:{
            OnlineNum,
            LineBox,
            MapBox,
            RangeBarBox
        }
    }
</script>

<style scoped>

</style>
