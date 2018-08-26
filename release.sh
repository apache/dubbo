# 创建新分支并checkout
# 检查tag是否存在，如果存在都删除？
# 调用release:prepare dryRun
# 调用 release:clean
# 调用 release:prepare
# 调用 release:clean恢复
# 到distribution目录下执行mvn clean install
# 拷贝target目录下zip包到svn目录
# 生成 sha512
# svn commit

# 检查tag是否存在
# 检查tag是否是SNAPSHOT版本
## 检查tag的commit是否和
# 检查签名是否正确
# 检查source中没有jar
# 检查source中没有空目录
# 输出source和tag目录内容的对比表格
# 检查LICENSE NOTICE DISCLAIMER存在