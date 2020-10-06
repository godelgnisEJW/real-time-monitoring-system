<template>
    <h2 style="display: flex;justify-content: center;margin-bottom: 16px;margin-top: 16px">
        当前时间为 <span style="color: #2db7f5"><i>{{ts}}</i></span> ,在线人数为 <animated-number
            :value="num"
            :formatValue="formatter"
            :duration="500"
            style="color: #2db7f5"
    /> 人
    </h2>
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
                num: 0,
                ts: "",
            }
        },
        methods: {
            formatter(value) {
                return Math.floor(value);
            }
        },
        mounted() {
            Bus.$on("ONLINE:NUM", message =>{
                let array = JSON.parse(message)
                let item = array.slice(-1)
                this.num = item[0].num
                this.ts = item[0].windowEnd
            })
        }
    }
</script>

<style scoped>

</style>