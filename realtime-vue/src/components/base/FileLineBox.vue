<template>
    <div>
        <Spin fix v-if="spinShow">
            <Icon type="ios-loading" size=18 class="spin-icon-load"/>
            <div>Loading</div>
        </Spin>
        <!-- 初始化echarts 需要有个带有宽高的盒子   -->
        <div ref="filelinebox" style="height: 300px;width: 600px"></div>
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
                        text: "实时文件数据统计",
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
                        name: "图片",
                        type: 'line',
                        // smooth:true,
                        showSymbol: false,
                        hoverAnimation: false,
                        // areaStyle: {
                        //     normal: {}
                        // },
                        data: null,
                    },{
                        name: "普通文件",
                        type: 'line',
                        // smooth:true,
                        showSymbol: false,
                        hoverAnimation: false,
                        // areaStyle: {
                        //     normal: {}
                        // },
                        data: null,
                    },{
                        name: "小视频",
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
            this.myecharts = echarts.init(this.$refs.filelinebox)
            Bus.$on("FILE:SIZE", message =>{
                let array = JSON.parse(message)
                let tmp = {'file-img':[],'file-normal':[],'file-video':[]}
                for (let i=0; i<array.length; i++){
                    for(let j=0; j < array[i].jsonResult.length; j++){
                        tmp[array[i].jsonResult[j].tp].push([array[i].windowEnd, (array[i].jsonResult[j].num/1048576).toFixed(0)])
                    }
                }
                this.option.series[0].data = tmp['file-img']
                this.option.series[1].data = tmp['file-normal']
                this.option.series[2].data = tmp['file-video']
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