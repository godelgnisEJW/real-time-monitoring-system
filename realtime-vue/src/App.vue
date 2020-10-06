<style scoped>
  .layout{
    border: 1px solid #d7dde4;
    background: #f5f7f9;
    position: relative;
    border-radius: 4px;
    overflow: hidden;
  }
  .layout-header-bar{
    background: #fff;
    box-shadow: 0 1px 1px rgba(0,0,0,.1);
  }
</style>
<template>
  <div class="layout">
    <Sider :style="{position: 'fixed', height: '100vh', left: 0, overflow: 'auto'}">
      <Menu theme='dark' active-name="1" >
        <MenuGroup title="实时指标统计分析">
          <router-link to="/hotIndex">
            <MenuItem name="hotIndex">
              <Icon type="md-flame" />
              热度指标
            </MenuItem>
          </router-link>
          <router-link to="/otherIndex">
            <MenuItem name="otherIndex">
              <Icon type="ios-podium-outline" />
              其他指标
            </MenuItem>
          </router-link>
          <router-link to="/userIndex">
            <MenuItem name="userIndex">
              <Icon type="md-contacts" />
              用户分析
            </MenuItem>
          </router-link>
        </MenuGroup>
      </Menu>
    </Sider>
    <Layout :style="{marginLeft: '200px'}">
      <Header :style="{background: '#fff', boxShadow: '0 2px 3px 2px rgba(0,0,0,.1)'}"></Header>
      <Content :style="{padding: '0 16px 16px'}">
        <Breadcrumb :style="{margin: '16px 0'}">
          <BreadcrumbItem>实时功能展示</BreadcrumbItem>
        </Breadcrumb>
        <Card>
          <div style="height: auto">
            <router-view></router-view>
          </div>
        </Card>
      </Content>
    </Layout>
  </div>
</template>
<script>

export default {
  name: 'App',
}
</script>
