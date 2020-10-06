<template>
    <div style="margin-left: 32px">
        <div id="flip-list-demo" class="demo">
            <Spin fix v-if="spinShow">
                <Icon type="ios-loading" size=18 class="spin-icon-load"/>
                <div>Loading</div>
            </Spin>
            <h1 style="display: flex;justify-content: center">城市排行榜</h1>
            <transition-group name="flip-list" >
                <Row class-name="rowmargin" type="flex" :gutter="16" v-for="item in items" v-bind:key="item.region">
                    <Col span="7">
                        <h5 style="display: flex;justify-content: flex-end">{{item.region}}</h5>
                    </Col>
                    <Col span="14" >
                        <Progress
                            :percent="item.percent"
                            :stroke-color="['#108ee9', '#87d068']"
                            :hide-info="true"
                            :stroke-width="20" >
                        </Progress>
                    </Col>
                    <Col span="1">
                        <animated-number
                                :value="item.num"
                                :formatValue="formatter"
                                :duration="1000"
                        />
                    </Col>

                </Row>

            </transition-group>
        </div>

    </div>
</template>

<script>
    import Bus from '../../api/eventBus'
    import AnimatedNumber from "animated-number-vue"
    export default {
        components: {
          AnimatedNumber,
        },
        data(){
            return{
                spinShow:true,
                items: []
            }
        },
        mounted() {
            Bus.$on("AREA:RANGE", message => {
                let array = JSON.parse(message)
                let max = array[0].num+1
                for (let i = 0; i < array.length; i++){
                    array[i].percent = (array[i].num * 100 / max)
                }
                this.items = array
                this.spinShow=false
            })
        },
        methods: {
            formatter(value) {
                return Math.floor(value);
            }
        }

    }
</script>

<style scoped>
    .flip-list-move {
        transition: transform 1s;
    }
    .rowmargin {
        margin-top: 8px;
        margin-bottom: 8px;
    }
    .spin-icon-load{
        animation: ani-demo-spin 1s linear infinite;
    }
</style>