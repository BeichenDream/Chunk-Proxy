# Chunk-Proxy

大多数Java web容器以及iis对http chunk支持的比较好  而且默认没有超时限制  并且支持双向流(输入、输出)同时运行
于是 Chunk-Proxy诞生了 但是有一个缺点如果遇到了反向代理 如nginx chunk-proxy就无法使用了
