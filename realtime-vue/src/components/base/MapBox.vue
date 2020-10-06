<template>
    <div>
        <Spin fix v-if="spinShow">
            <Icon type="ios-loading" size=18 class="spin-icon-load"/>
            <div>Loading</div>
        </Spin>
        <!-- 初始化echarts 需要有个带有宽高的盒子   -->
        <div ref="mapbox" style="height: 600px;width: 100%;margin-bottom: 16px;margin-top: 16px"></div>
    </div>
</template>

<script>
    import echarts from 'echarts'
    import Bus from '../../api/eventBus'
    import 'echarts/map/js/china'
    import geos from '../../assets/geos'
    export default {
        data(){
            return{
                spinShow:true,
                myecheats: null,
                option: {
                    backgroundColor: '#032C4B',
                    title:{
                        text: "在线用户分布图",
                        left: 'center',
                        textStyle: {
                            color: '#fff'
                        }
                    },
                    tooltip:{
                        trigger:'item',
                        formatter:'{a}<br/>{b}:{c}'
                    },
                    legend: {
                        orient: 'vertical',
                        top: 'bottom',
                        left: 'right',
                        data:['hot'],
                        textStyle: {
                            color: '#fff'
                        }
                    },
                    visualMap: {
                        min: 0,
                        max: 100000000,
                        calculable: true,
                        inRange: {
                            color: [ '#034499', '#0C3FA2','#0971FC' ,'#C1DEFA','#9EEAFF', '#2CE8FF']
                        },
                        textStyle: {
                            color: '#fff'
                        }
                    },
                    geo: {
                        map: 'china',
                        itemStyle: {
                            areaColor: '#040811',
                            borderColor: '#111'
                        },
                        emphasis: {
                            itemStyle: {
                                areaColor: '#040811'
                            },
                            label: {
                                show: false
                            }
                        }
                    },
                    series: [{
                        name: "Hot",
                        type: 'scatter',
                        coordinateSystem: 'geo',
                        data:null,
                        symbolSize: 3,
                        itemStyle: {
                            opacity: 0.4
                        },
                        emphasis: {
                            itemStyle: {
                                borderColor: '#fff',
                                borderWidth: 1
                            }
                        }
                    }]
                },
            }
        },
        mounted() {
            this.myecharts = echarts.init(this.$refs.mapbox)
            //console.log(geos)
            Bus.$on("AREA:HOT", message =>{
                let array = JSON.parse(message)
                // console.log(array)
                this.option.series[0].data = this.convertData(array)
                this.myecharts.setOption(this.option)
                this.spinShow = false
            })
        },
        methods: {
            convertData(data) {
                let res = [];
                for (let i = 0; i < data.length; i++) {
                    let geoCoord = geos[data[i].region];
                    if (geoCoord) {
                        res.push({
                            name: data[i].region,
                            value: geoCoord.concat(data[i].num)
                        });
                    }
                }
                return res;
            }
        }
    }
</script>

<style scoped>
    .spin-icon-load{
        animation: ani-demo-spin 1s linear infinite;
    }
</style>