<template>
    <div>
        <Spin fix v-if="spinShow">
            <Icon type="ios-loading" size=18 class="spin-icon-load"/>
            <div>Loading</div>
        </Spin>
        <!-- 初始化echarts 需要有个带有宽高的盒子   -->
        <div ref="interfacelinebox" style="height: 300px;width: 600px"></div>
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
                        text: "实时接口请求数据统计",
                        left: 'center'
                    },
                    legend:{
                        right: '10%',
                        orient: 'vertical'
                    },
                    tooltip: {
                        trigger: 'axis',
                        // formatter: '{b} {c}',
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
                        name: "server1",
                        type: 'line',
                        // smooth:true,
                        showSymbol: false,
                        hoverAnimation: false,
                        // areaStyle: {
                        //     normal: {}
                        // },
                        data: null,
                    },{
                        name: "server2",
                        type: 'line',
                        // smooth:true,
                        showSymbol: false,
                        hoverAnimation: false,
                        // areaStyle: {
                        //     normal: {}
                        // },
                        data: null,
                    },{
                        name: "server3",
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
            this.myecharts = echarts.init(this.$refs.interfacelinebox)
            Bus.$on("INTERFACE:NUM", message =>{
                let array = JSON.parse(message)
                let tmp = {'server1':[],'server2':[],'server3':[]}
                for (let i=0; i<array.length; i++){
                    for(let j=0; j < array[i].jsonResult.length; j++){
                        tmp[array[i].jsonResult[j].serverId].push([array[i].ts, array[i].jsonResult[j].num])
                    }
                }
                this.option.series[0].data = tmp['server1']
                this.option.series[1].data = tmp['server2']
                this.option.series[2].data = tmp['server3']
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