/**********************************************************************
*	湖南长沙阳环科技实业有限公司
*	@Copyright (c) 2003-2017 yhcloud, Inc. All rights reserved.
*	
*	This copy of Ice is licensed to you under the terms described in the
*	ICE_LICENSE file included in this distribution.
*	
*	@license http://www.yhcloud.com.cn/license/
**********************************************************************/
package com.yhcloud.pdftool.logic;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.BlockElement;
import com.itextpdf.layout.element.IElement;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.layout.font.FontProvider;

/** 
 * 生成pdf工具
 *
 * @author leig
 * @version 20170301
 *
 */
public class PdfUtil {

	private final static Logger log = LoggerFactory.getLogger(PdfUtil.class);
	
	// 模板访问地址
	private String templatePath;
	
	/**
	 * 默认模板路径是 http://192.168.0.139/edu/m/m06/m0601/homework.html
	 */
	public PdfUtil() {
		this("http://192.168.0.139/edu/m/m06/m0601/homework.html");
	}
	
	public PdfUtil(String templatePath) {
		this.templatePath = templatePath;
	}

	public void setTemplatePath(String templatePath) {
		this.templatePath = templatePath;
	}

	/**
	 * 默认生成pdf文件在当前目录
	 * 
	 * @param contents Map<String, String>
	 * @return
	 */
	public Map<String, String> execute(Map<String, String> contents) {
		return execute(contents, "target.html", "target.pdf", ".");
	}
	
	/**
	 * 执行操作生成pdf相关文件
	 * 
	 * @param contents 习题题干集合  Map<String, String>
	 * @param templateHtml html文件生成的全路径 /xx/xx/xx.html
	 * @param pdfPath pdf文件生成的全路径 /xx/xx/xx.pdf
	 * @param imageDir 保存image图片的目录 /xx/xx/
	 * @return
	 */
	public Map<String, String> execute(Map<String, String> contents, String templateHtml, String pdfPath, String imageDir) {
		// 构建本地模板文件
		File templateFile = new File(templateHtml);
		
		if (templateFile.exists()) {
			log.info("删除临时文件操作: " + templateFile.delete());
		}
		
		try {
			
			URL url = new URL(templatePath);
			FileUtils.copyURLToFile(url, templateFile);
			
		} catch (IOException e) {
			e.printStackTrace();
			log.error("捕获异常: " + e.getMessage());
		}
		
		// 准备返回参数
		Map<String, String> map = new HashMap<>();
		
		try {
			
			String htmlPath = createHtml(templateFile, contents);
			
			map.put("Html", htmlPath);
			
			String targetPath = convert(htmlPath, pdfPath);
			
			map.put("LatticePenPDF", targetPath);
			
			List<String> images = pdfToImage(pdfPath, imageDir);

			if (null != images) {

                StringBuilder str = new StringBuilder();

                for (String image : images) {
                    str.append(image).append("&");
                }

                map.put("Images", str.toString());
            }
			
		} catch(Exception e) {
			e.printStackTrace();
			log.error("捕获异常: " + e.getMessage());
		}
		
		return map;
	}
	
    /**
     * 通过字符串集合生成目标文件
     *
     * @param contents
     */
    private String createHtml(File file, Map<String, String> contents) {

        if (0 >= contents.size() || "".equals(searchContent())) {
            return null;
        }

        String searchText = searchContent();

        try {
            // 创建文件输入流
            FileReader fis = new FileReader(file);
            // 创建缓冲字符数组
            char[] data = new char[1024];
            int rn;
            // 创建字符串构建器
            StringBuilder sb = new StringBuilder();
            // 读取文件内容到字符串构建器
            while (0 < (rn = fis.read(data))) {
                String str = String.valueOf(data, 0, rn);
                sb.append(str);
            }
            // 关闭输入流
            fis.close();

            StringBuilder replaceText = new StringBuilder();

            for (Map.Entry<String, String> entry : contents.entrySet()) {
            	String div = "<div class='QID-" + entry.getKey() + "'>";
            	replaceText.append(div)
            	.append(entry.getValue())
            	.append("</div>")
            	.append(System.lineSeparator());
            }

            File htmlFile = new File("target.html");

            if (htmlFile.exists()) {
                log.info("删除原始文件结果: " + htmlFile.delete());
            }

            // 从构建器中生成字符串，并替换搜索文本
            String str = sb.toString().replace(searchText, replaceText.toString());
            // 创建文件输出流
            FileWriter fout = new FileWriter(htmlFile);
            // 把替换完成的字符串写入文件内
            fout.write(str.toCharArray());
            // 关闭输出流
            fout.close();

            return htmlFile.getAbsolutePath();

        } catch (IOException e) {
            e.printStackTrace();
            log.error("捕获异常: " + e.getMessage());
        }

        return null;
    }

    /**
     * 模板替换格式
     * 
     * @return
     */
    private String searchContent() {
        return "[*][#][*]";
    }

    /**
     * 将html生成pdf
     * 
     * @param htmlPath
     * @return
     */
    private String convert(String htmlPath, String pdfPath) {

        try {

        	File html = new File(htmlPath);

            if (!html.exists()) {
                log.error("源html文件不存在...");
                return null;
            }

            String content = fileToString(html);

            if (null == content) {
                log.error("转码失败: " + htmlPath);
                return null;
            }

            File pdfFile = new File(pdfPath);

            if (pdfFile.exists()) {
                log.info("删除旧pdf文件: " + pdfFile.delete());
            } else if (null != pdfFile.getParentFile() && !pdfFile.getParentFile().exists()) {
                log.info("创建pdf文件目录: " + pdfFile.getParentFile().mkdirs());
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDocument = new PdfDocument(writer);

            ConverterProperties props = new ConverterProperties();

            // 提供解析用的字体
            FontProvider fp = new FontProvider();
            // 添加标准字体库、无中文
            fp.addStandardPdfFonts();
            // 添加系统字体库
            fp.addSystemFonts();

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            // 自定义字体路径、解决中文,可先用绝对路径测试。
            URL url = classLoader.getResource("fonts");
            if (null != url) {
                String fontsPath = url.getPath();
                fp.addDirectory(fontsPath);
            }

            props.setFontProvider(fp);

            List<IElement> iElements = HtmlConverter.convertToElements(content, props);

            // immediateFlush设置true和false都可以，false 可以使用 relayout
            Document document = new Document(pdfDocument, PageSize.A4, true);
            document.setMargins(0, 0, 0, 0);

            for (IElement iElement : iElements) {
                BlockElement blockElement = (BlockElement) iElement;
                blockElement.setMargins(0, 0, 0, 0);
                document.add(blockElement);
            }

            document.close();

            FileOutputStream fileOutputStream = new FileOutputStream(pdfFile);
            fileOutputStream.write(outputStream.toByteArray());
            fileOutputStream.close();

            return pdfFile.getAbsolutePath();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    
    private List<String> pdfToImage(String pdfPath, String imageDir) {
    	
    	File file = new File(pdfPath);
    	
    	if (!file.exists()) {
    		log.error("pdf文件不存在...");
    		return null;
    	}
    	
    	File imageDirFile = new File(imageDir);
    	
    	if (!imageDirFile.exists()) {
    		log.info("创建图片路径操作: " + imageDirFile.mkdirs());
    	}
    	
        try {
        	
            PDDocument doc = PDDocument.load(file);
            PDFRenderer renderer = new PDFRenderer(doc);
            int pageCount = doc.getNumberOfPages();
            
            List<String> images = new ArrayList<>();
            
            for(int i = 0; i < pageCount; i++) {
            	// Windows native DPI
                BufferedImage image = renderer.renderImageWithDPI(i, 1200);
                // 产生缩略图
                BufferedImage srcImage = resize(image, 2479, 3508);
                File imageFile = new File(imageDir, file.getName() + "_" + i + ".png");
                ImageIO.write(srcImage, "PNG", imageFile);
                
                images.add(imageFile.getAbsolutePath());
            }
            
            return images;
        } catch (IOException e) {
            e.printStackTrace();
            log.error("捕获异常: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 生成图片
     * 
     * @param source
     * @param targetW
     * @param targetH
     * @return
     */
    private BufferedImage resize(BufferedImage source, int targetW,  int targetH) {
        int type = source.getType();
        BufferedImage target;
        double sx = (double)targetW / source.getWidth();
        double sy = (double)targetH / source.getHeight();
        if(sx > sy){
            sx = sy;
            targetW = (int)(sx * source.getWidth());
        } else {
            sy = sx;
            targetH = (int)(sy * source.getHeight());
        }
        if(type == BufferedImage.TYPE_CUSTOM) {
            ColorModel cm = source.getColorModel();
            WritableRaster raster = cm.createCompatibleWritableRaster(targetW, targetH);
            boolean alphaPremultiplied = cm.isAlphaPremultiplied();
            target = new BufferedImage(cm,raster,alphaPremultiplied,null);
        } else {
            target = new BufferedImage(targetW, targetH,type);
        }
        Graphics2D g = target.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawRenderedImage(source, AffineTransform.getScaleInstance(sx, sy));
        g.dispose();

        return target;
    }

    /**
     * 文件内容转字符串
     *
     * @param source
     * @return
     */
    private String fileToString(File source) {

        String content = null;

        try {

            FileInputStream inStream = new FileInputStream(source);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            byte[] buffer = new byte[2048];

            int length;

            while(-1 != (length = inStream.read(buffer))) {
                bos.write(buffer, 0, length);
            }

            bos.close();
            inStream.close();

            content = bos.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return content;
    }
}
