# 期中实验：NotePad笔记本应用
## 一、实验题目
基于NotePad应用做功能扩展
## 二、实验内容
1.下载NotePad源码：[https://github.com/llfjfz/NotePad](https://github.com/llfjfz/NotePad)  
2. 阅读NotePad的源代码并做如下扩展：  
◼ 基本要求：每个人必须完成    
 NoteList中显示条目增加时间戳显示  
 添加笔记查询功能（根据标题查询）  
◼ 附加功能：根据自身实际情况进行扩充，以下是建议的扩展功能（拟作
为期末作业）  
 UI美化  
 更改记事本的背景  
 导出笔记，笔记的云备份和恢复  
 添加代办功能  
 记事本的设置的功能  
 笔记分类  
 支持多种资源，如保存图片、语音、视频等（参考印象笔记）  
 语音搜索？  
 笔记便签  
 偏好设置  
 。。。。。。  
## 三、实验过程
### 1.时间戳显示
在noteslist_item.xml里添加一个TextView,用来显示时间戳  
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201220213357333.PNG?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0FOQklOQUlOQQ==,size_16,color_FFFFFF,t_70)  
在NotePadProvider.java中的READ_NOTE_PROJECTION添加显示时间的字段：   
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201220214326556.PNG)  
在NotesList.java的PROJECTION也添加显示时间的字段：   
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201220214342730.PNG)  
在NotesList.java的dataColumns，viewIDs中补充时间部分：   
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201220214356555.PNG)  
设置时间戳的时间格式，在NotePadProvider.java中的insert方法和NoteEditor.java中的updateNote方法，加入设置的代码:   
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201220215731452.png)  
运行结果：   
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201220220326358.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0FOQklOQUlOQQ==,size_16,color_FFFFFF,t_70)  
### 2.添加笔记查询功能
在list_options_menu.xml中添加搜索的item：   
![在这里插入图片描述](https://img-blog.csdnimg.cn/2020122022075945.png)  
在layout中建note_search_list.xml文件，实现搜索框以及页面：   
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201220221305593.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0FOQklOQUlOQQ==,size_16,color_FFFFFF,t_70)  
在NotesList.java的onOptionsItemSelected方法里的switch添加搜索case语句：   
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201220222006652.png)
建一个class，NoteSearch,用来实现对搜索框的监听：（主要是监听器的注册和这2个方法的实现）  
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201220222902342.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0FOQklOQUlOQQ==,size_16,color_FFFFFF,t_70)
最后在AndroidManifest.xml注册NoteSearch：  
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201220223928198.png)
运行结果：   
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201220224147223.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0FOQklOQUlOQQ==,size_16,color_FFFFFF,t_70)
### 3.扩展---可设置背景颜色
在系统中定义五种颜色，根据颜色对应不int值选择要显示的颜色（契约类NotePad中的定义)：   
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201220224951297.png)  
在NotePadProvider.java中写一个addColumn方法，在表notes中加入记录背景颜色的列color(操作步骤： 1、先更改表名 2、创建新表,表名为原来的表名 3、复制数据 4、删除旧表):   
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201220225957937.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0FOQklOQUlOQQ==,size_16,color_FFFFFF,t_70)  
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201220230206934.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0FOQklOQUlOQQ==,size_16,color_FFFFFF,t_70)  
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201220230238558.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0FOQklOQUlOQQ==,size_16,color_FFFFFF,t_70)  
自定义一个CursorAdapter，完成cursor读取的数据库内容填充到item，然后填充颜色，并将NoteList中用的SimpleCursorAdapter改使用MyCursorAdapter：（case语句要写完所有定义的颜色）  
![在这里插入图片描述](https://img-blog.csdnimg.cn/2020122023071964.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0FOQklOQUlOQQ==,size_16,color_FFFFFF,t_70)  
在editor_options_menu.xml添加修改背景颜色选项，并且在NoteEditor中的onOptionsItemSelected()方法的switch中添加这个选项:（让选项在菜单出现）  
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201220231243102.png)  
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201220231338822.png)  
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201220231429435.png)  
创建note_color.xml，NoteColor的Acitvity，用来选择颜色。并在AndroidManifest.xml中将NoteColor主题定义为对话框样式（即实现弹出对话框来选颜色）：   
*用线性布局多个ImageButton组件  
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201220231808552.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0FOQklOQUlOQQ==,size_16,color_FFFFFF,t_70)  
*主要实现onPause方法，将选择的方法存入数据库：   
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201220232349749.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0FOQklOQUlOQQ==,size_16,color_FFFFFF,t_70)  
*定义对话框：   
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201220232746456.png)  
运行结果：   
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201220233628831.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0FOQklOQUlOQQ==,size_16,color_FFFFFF,t_70)  
### 4.扩展---笔记分类
想法：用NotePadProvider.java里的query方法，投影笔记的color,将拥有相同color属性的笔记在同一组件进行展示，实现笔记分类。
## 四、实验结果
主界面：   
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201220235153535.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0FOQklOQUlOQQ==,size_16,color_FFFFFF,t_70)  
点击菜单：   
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201220235343619.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0FOQklOQUlOQQ==,size_16,color_FFFFFF,t_70)  
点击Search:   
![在这里插入图片描述](https://img-blog.csdnimg.cn/2020122023550630.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0FOQklOQUlOQQ==,size_16,color_FFFFFF,t_70)  
输入搜索：   
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201220235628596.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0FOQklOQUlOQQ==,size_16,color_FFFFFF,t_70)  
返回主界面，点击菜单，点击New note,输入笔记内容，点击菜单：  
![在这里插入图片描述](https://img-blog.csdnimg.cn/2020122023595868.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0FOQklOQUlOQQ==,size_16,color_FFFFFF,t_70)  
点击改变背景颜色，选择颜色：  
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201221000117687.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0FOQklOQUlOQQ==,size_16,color_FFFFFF,t_70)  
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201221000219829.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0FOQklOQUlOQQ==,size_16,color_FFFFFF,t_70)  
![在这里插入图片描述](https://img-blog.csdnimg.cn/2020122100033624.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0FOQklOQUlOQQ==,size_16,color_FFFFFF,t_70)  
点击feuwgw进入Edit:feuwgw，菜单同样可以该背景颜色：   
![在这里插入图片描述](https://img-blog.csdnimg.cn/20201221000720935.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0FOQklOQUlOQQ==,size_16,color_FFFFFF,t_70)  
![在这里插入图片描述](https://img-blog.csdnimg.cn/2020122100083896.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L0FOQklOQUlOQQ==,size_16,color_FFFFFF,t_70)  


