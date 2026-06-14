# 云盘链接下载插件

## 交流群

[点击链接加入群聊【halo博客-lywq插件】](https://qm.qq.com/q/wuC7NZr0sw)

<img src="https://github.com/user-attachments/assets/bf162401-07fd-49ec-b50f-5218c9510937" style="height: 400px !important; width: auto; object-fit: contain;" />

## 简介

一款为Halo提供云盘下载链接卡片展示的插件，支持默认编辑器中可视化插入网盘链接、选择 Halo 附件作为下载文件，也支持在其他 Markdown / HTML 编辑器中通过自定义标签插入下载卡片。

## 默认编辑器用法

在 Halo 默认编辑器中插入下载信息后，可以手动填写下载地址，也可以点击“附件”按钮从后台附件库中选择文件。选择附件后会自动回填下载地址和文件名，并将下载来源标记为“附件”。

## 其他编辑器用法

如果你使用的不是 Halo 默认编辑器，可以在文章或页面中插入以下 HTML：

```html
<download-links>
  <download-link
    source="百度云网盘"
    filename="示例文件.zip"
    url="https://example.com/file.zip"
    code="abcd"
  ></download-link>
  <download-link
    source="夸克网盘"
    filename="备用下载地址"
    url="https://example.com/backup.zip"
  ></download-link>
</download-links>
```

字段说明：

- `source`：下载来源名称，和插件设置里的下载来源名称一致时会自动匹配图标
- `filename`：下载文件名或展示标题
- `url`：下载地址，必填
- `code`：提取码，可选

如果 Markdown 编辑器将上述标签转义为 `&lt;download-links&gt;` 形式，插件也会在前台渲染时识别并转换。

## 展示
<img style="width: 400px !important; height: auto; object-fit: contain;" src="https://github.com/user-attachments/assets/c33ceb01-fbe4-4540-a902-d7ef4995478d" />
<img style="width: 400px !important; height: auto; object-fit: contain;" src="https://github.com/user-attachments/assets/f6c28e31-9503-4d6b-ac8a-c8413e61a96b" />
<img style="width: 800px !important; height: auto; object-fit: contain;" src="https://github.com/user-attachments/assets/3b692bda-7319-47c1-8093-23d03e9aecdb" />


## 开发环境

- Java 21+
- Node.js 18+
- pnpm

## 开发

```bash
# 启用插件
./gradlew haloServer
# 开发前端
cd ui
pnpm install
pnpm dev
```

## 构建

```bash
./gradlew build
```

构建完成后，可以在 `build/libs` 目录找到插件 jar 文件。

## 许可证

[GPL-3.0](./LICENSE) © lywq 
