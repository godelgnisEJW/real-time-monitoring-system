import axios from 'axios';
axios.defaults.withCredentials=true;

axios.interceptors.response.use((res)=>{
    return res.data;
})

export let getRedisData = (key) => {
    return axios.get(`/getValue/${key}`)
}