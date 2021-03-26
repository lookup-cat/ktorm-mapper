### 支持ktorm框架的一对多 一对一映射

- 添加依赖

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
dependencies {
    implementation("com.github.641571835:ktorm-mapper:1.3")
}
```

- 使用示例

```kotlin
  val list = database.from(Users)
    .innerJoin(UserRoles, Users.id eq UserRoles.userId)
    .innerJoin(Roles, UserRoles.roleId eq Roles.id)
    .innerJoin(RolePermissions, Roles.id eq RolePermissions.roleId)
    .innerJoin(Permissions, RolePermissions.permissionId eq Permissions.id)
    .leftJoin(Assets, Assets.userId eq Users.id)
    .select(Users.columns + Roles.columns + Permissions.columns + Assets.columns)
    .mapMany(Users.rowSetMapper(Users.id) {
        hasMany(Roles, User::roles, Roles.id) {
            hasMany(Permissions, Role::permissions, Permissions.id)
        }
        hasOne(Assets, User::asset, Assets.id)
    })
val json = objectMapper.writeValueAsString(list)
println(json)
```

- 输出结果

```json
[
  {
    "id": 1,
    "username": "张三",
    "roles": [
      {
        "id": 1,
        "name": "管理员",
        "permissions": [
          {
            "id": 1,
            "name": "用户管理"
          }
        ]
      }
    ],
    "asset": {
      "id": 1,
      "name": "房子",
      "userId": 1
    }
  }
]
```
