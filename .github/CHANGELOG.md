- 修复正式版本点击“程序库”页面时因 aboutlibraries 元数据被资源压缩移除而闪退的问题
- 改为显式加载 aboutlibraries 元数据资源，确保程序库列表在 release 构建中正常显示

<details>
<summary>English Version</summary>

- Fixed a crash when opening the Libraries page in release builds after the aboutlibraries metadata was removed by resource shrinking
- Switched to explicitly loading the aboutlibraries metadata resource so the Libraries list renders correctly in release builds

</details>