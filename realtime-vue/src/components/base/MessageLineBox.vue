<template>
    <div>
        <Spin fix v-if="spinShow">
            <Icon type="ios-loading" size=18 class="spin-icon-load"/>
            <div>Loading</div>
        </Spin>
        <!-- 初始化echarts 需要有个带有宽高的盒子   -->
        <div ref="messagelinebox" style="height: 300px;width: 600px"></div>
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
                        text: "实时消息数据统计",
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
                        // scale: true
                    },
                    series: [{
                        name: "文本消息",
                        type: 'line',
                        // smooth:true,
                        showSymbol: false,
                        hoverAnimation: false,
                        // areaStyle: {
                        //     normal: {}
                        // },
                        data: null,
                    },{
                        name: "语音消息",
                        type: 'line',
                        // smooth:true,
                        showSymbol: false,
                        hoverAnimation: false,
                        // areaStyle: {
                        //     normal: {}
                        // },
                        data: null,
                    },{
                        name: "表情包",
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
            this.myecharts = echarts.init(this.$refs.messagelinebox)
            Bus.$on("MSG:NUM", message =>{
                let array = JSON.parse(message)
                let tmp = {'msg-text':[],'msg-radio':[],'msg-emoj':[]}
                for (let i=0; i<array.length; i++){
                    for(let j=0; j < array[i].jsonResult.length; j++){
                        tmp[array[i].jsonResult[j].tp].push([array[i].windowEnd, array[i].jsonResult[j].num])
                    }
                }
                this.option.series[0].data = tmp['msg-text']
                this.option.series[1].data = tmp['msg-radio']
                this.option.series[2].data = tmp['msg-emoj']
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