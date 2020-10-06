<template>
    <div>
        <Spin fix v-if="spinShow">
            <Icon type="ios-loading" size=18 class="spin-icon-load"/>
            <div>Loading</div>
        </Spin>
        <!-- 初始化echarts 需要有个带有宽高的盒子   -->
        <div ref="linebox" style="height: 300px;width: 700px"></div>
    </div>
</template>

<script>
    import echarts from 'echarts'
    import Bus from '../../api/eventBus'
    export default {
        data(){
            return{
                spinShow:true,
                myecheats: null,
                option: {
                    title:{
                        text: "当前在线人数动态图",
                        left: 'center'
                    },
                    tooltip: {
                        trigger: 'axis',
                        formatter: '{b} {c}',
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
                        name: "实时在线人数数据展示",
                        type: 'line',
                        // smooth:true,
                        showSymbol: false,
                        hoverAnimation: false,
                        areaStyle: {
                            normal: {}
                        },
                        data: null,
                    }]
                },
            }
        },
        mounted() {
            this.myecharts = echarts.init(this.$refs.linebox)
            Bus.$on("ONLINE:NUM", message =>{
                let array = JSON.parse(message)
                let lineData = array.map(x => [x.windowEnd, x.num])
                // console.log(lineData)
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