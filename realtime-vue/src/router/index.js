import HotIndex from "../components/page/HotIndex";
import OtherIndex from "../components/page/OtherIndex";
import UserIndex from "../components/page/UserIndex";
export default  [
    {
        path:'/',
        redirect:'/hotIndex'
    },
    {
        path:'/hotIndex',
        component:HotIndex
    },
    {
        path: '/otherIndex',
        component: OtherIndex
    },
    {
        path: '/userIndex',
        component: UserIndex
    },
        //     children:[
        //         {
        //             path:'/backstage/user',
        //             component:()=>import('../components/UserManager.vue'),
        //             meta:{ keepAlive:true, title:'用户管理',requireAuth:true }
        //         }]
        // },
        {
            path:'*',
            redirect:'/hotIndex'},
]