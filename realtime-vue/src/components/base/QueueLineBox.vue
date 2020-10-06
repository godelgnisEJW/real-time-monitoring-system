<template>
    <div>
        <Spin fix v-if="spinShow">
            <Icon type="ios-loading" size=18 class="spin-icon-load"/>
            <div>Loading</div>
        </Spin>
        <!-- 初始化echarts 需要有个带有宽高的盒子   -->
        <div ref="queuelinebox" style="height: 300px;width: 600px"></div>
    </div>
</template>

<script>
    import echarts from 'echarts'
    import Bus from '../../api/eventBus'
    export default {
        data(){
            return{
                spinShow: true,
                myecheats: null,
                option: {
                    title:{
                        text: "实时队列消息堆积统计",
                        left: 'center'
                    },
                    legend:{
                        right: '10%',
                        orient: 'vertical'
                    },
                    tooltip: {
                        trigger: 'axis',
                        axisPointer: {
                            animation: false
                        }
                    },
                    grid: {
                        left: '13%',
                    },
                    xAxis: {
                        type: 'time',
                        splitLine: {
                            show: false
                        },
                        maxInterval: 1000
                    },
                    yAxis: {
                        type: 'value',
                        boundaryGap: [0, '50%'],
                        splitLine: {
                            show: false
                        },
                        scale: true
                    },
                    series: [{
                        name: "消息堆积",
                        type: 'line',
                        // smooth:true,
                        showSymbol: false,
                        hoverAnimation: false,
                        // areaStyle: {
                        //     normal: {}
                        // },
                        data: null,
                    }]
                },
            }
        },
        mounted() {
            this.myecharts = echarts.init(this.$refs.queuelinebox)
            Bus.$on("QUEUE:PILEUP:NUM", message =>{
                let array = JSON.parse(message)
                let lineData = array.map(x => [x.windowEnd, x.pileUpNum])
                // console.log(array)
                this.option.series[0].data = lineData
                this.myecharts.setOption(this.option)
                this.spinShow = false
            })
        },
    }
</script>

<style scoped>
    .spin-icon-load{
        animation: ani-demo-spin 1s linear infinite;
    }
</style>