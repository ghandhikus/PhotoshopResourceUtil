package psdutil;

import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Iterator;

import com.twelvemonkeys.imageio.plugins.psd.PSDImageReader;
import com.twelvemonkeys.imageio.plugins.psd.PSDMetadata;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ConverterCore {

    public static void main(String[] args) throws IOException {
        if(args.length == 0) throw new InvalidParameterException("Usage is:\napp.jar file.psd \"Output Folder\"");
        final String FILE_NAME = args[0];
        final String OUTPUT_FOLDER = args[1];
        System.out.println(FILE_NAME);

        final String[] p = FILE_NAME.split("\\\\");
        final String OBJECT_NAME = p[p.length-1].split("\\.")[0]; // < filename without extension

        // Create input stream (in try-with-resource block to avoid leaks)
        try (ImageInputStream input = ImageIO.createImageInputStream(new File(FILE_NAME))) {
            // Get the reader
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);

            if (!readers.hasNext()) {
                throw new IllegalArgumentException("No reader for: " + FILE_NAME);
            }

            ImageReader reader = readers.next();

            if(!(reader instanceof PSDImageReader)) {
                throw new RuntimeException("File isn't being read as PSD!");
            }

            PSDImageReader psd = (PSDImageReader) reader;



            try {
                reader.setInput(input);
                int width = psd.getWidth(0);
                int height = psd.getHeight(0);



                var outputDir = new File(OUTPUT_FOLDER);
                if(!outputDir.exists()) {
                    if(!outputDir.mkdirs())
                        throw new RuntimeException("Couldn't create output folder named: "+OUTPUT_FOLDER);
                }


                int layerCount = psd.getNumImages(true);
                ImageReadParam param = reader.getDefaultReadParam();

                var basePos = (PSDMetadata)psd.getImageMetadata(0);

//                iteratePrint(basePos.getAsTree("com_twelvemonkeys_imageio_psd_image_1.0").getChildNodes());

                ArrayList<LayerInfo> photoshopLayers = findLayerInfo(basePos, layerCount-1);

                final int BASE_LAYER = 1;

                var base = psd.read(BASE_LAYER, param);
                var baseLayer = photoshopLayers.get(BASE_LAYER-1);

                LayerGroup curLayerGroup = null;
                int groupDepth = 0;

                for (int i = 0; i < photoshopLayers.size(); i++) {
                    var layer  = photoshopLayers.get(i);
                    boolean newGroup = layer.name.equalsIgnoreCase("</Layer_group>") || layer.name.equalsIgnoreCase("</Layer group>");
                    boolean endGroup = !newGroup && layer.pixelDataIrrelevant;

                    // Create groups all the time until layer group has been found
                    if(groupDepth==0) {
                        curLayerGroup = new LayerGroup(psd, base, baseLayer, width, height);
                    }
                    curLayerGroup.name = layer.name;

                    if(newGroup) {
                        groupDepth++;
                    }

                    if(endGroup) {
                        groupDepth--;
                    }


                    if(!layer.pixelDataIrrelevant && !layer.ignore) {
                        var overlay = psd.read(i+1, param);
                        curLayerGroup.objects++;
                        curLayerGroup.graph.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, layer.opacity/255f));
                        curLayerGroup.graph.drawImage(overlay, layer.x, layer.y, layer.w, layer.h, null);
                    }

                    System.out.println(groupDepth+" "+photoshopLayers.get(i).toString());
                    if(groupDepth == 0 && curLayerGroup.objects>0) {
                        StringBuilder sb = new StringBuilder(OUTPUT_FOLDER);
                        sb.append("\\");
                        sb.append(OBJECT_NAME);
                        if(!curLayerGroup.name.equalsIgnoreCase("Background") && !curLayerGroup.name.equals(OBJECT_NAME)) {
                            if(Settings.ADD_DASH_BEFORE_VARIATIONS)
                                sb.append("_");
                            if(Settings.TURN_LAYERS_LOWER_CASE)
                                sb.append(curLayerGroup.name.toLowerCase());
                        }
                        sb.append(".png");
                        writeImage(sb.toString(), curLayerGroup.img);
                    }
//                    System.out.println(photoshopLayers.get(i).toString());
                }


            } finally {
                reader.dispose();
            }
        }

    }

    static class LayerGroup {
        final Graphics2D graph;
        final BufferedImage img;
        String name = "";
        int objects = 0;

        LayerGroup(PSDImageReader psd, BufferedImage base, LayerInfo baseLayer, int w, int h) throws IOException {
            this.img = psd.getRawImageType(0).createBufferedImage(w, h);
            this.graph = img.createGraphics();
            graph.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, baseLayer.opacity/255f));
            graph.drawImage(base, baseLayer.x, baseLayer.y, baseLayer.w, baseLayer.h, null);
        }


        void calc() {
            if(Settings.REPLACE_LAYER_NAME_SPACES_TO_DASHES)
                this.name = name.toLowerCase().replace(" ", "_");
        }

        @Override
        public String toString() {
            return "LayerGroup{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }


    static class LayerInfo {
        boolean ignore;
        String name;
        int top;
        int left;
        int bottom;
        int right;
        String blendMode;
        int opacity;
        String clipping;
        String flags;
        boolean pixelDataIrrelevant = false;

        int x,y,w,h;
                
        LayerInfo()
        {

        }

        void calc() {
            this.x = left;
            this.y = top;
            this.h = bottom-top;
            this.w = right-left;
            if(Settings.REPLACE_LAYER_NAME_SPACES_TO_DASHES)
                this.name = name.replace(" ", "_");
        }

        @Override
        public String toString() {
            return "LayerInfo{" +
                    "name='" + name + '\'' +
                    ", top=" + top +
                    ", left=" + left +
                    ", bottom=" + bottom +
                    ", right=" + right +
                    ", blendMode='" + blendMode + '\'' +
                    ", opacity=" + opacity +
                    ", clipping='" + clipping + '\'' +
                    ", flags='" + flags + '\'' +
                    ", pixelDataIrrelevant=" + pixelDataIrrelevant +
                    ", x=" + x +
                    ", y=" + y +
                    ", w=" + w +
                    ", h=" + h +
                    '}';
        }
    }

    public static ArrayList<LayerInfo> findLayerInfo(PSDMetadata basePos, int limit) {
        return _findLayerInfo(basePos.getAsTree("com_twelvemonkeys_imageio_psd_image_1.0").getChildNodes(), new ArrayList<>(), limit);
    }
    
    private static ArrayList<LayerInfo> _findLayerInfo(NodeList list, ArrayList<LayerInfo> ret, int limit) {
        // Limit deep nested structure copies to layer limit
        if(ret.size() == limit)
            return ret;

        for (int i = 0; i < list.getLength(); i++) {
            var node = list.item(i);

            if(node.getNodeName().equals("LayerInfo")) {
                LayerInfo info = new LayerInfo();
                ret.add(info);
                if (node.hasAttributes()) {
                    var ats = node.getAttributes();
                    for (int j = 0; j < ats.getLength(); j++) {
                        var att = ats.item(j);
                        
                        switch(att.getNodeName()) {
                            case "name": info.name = att.getNodeValue(); break;
                            case "top": info.top = Integer.parseInt(att.getNodeValue()); break;
                            case "left": info.left = Integer.parseInt(att.getNodeValue()); break;
                            case "bottom": info.bottom = Integer.parseInt(att.getNodeValue()); break;
                            case "right": info.right = Integer.parseInt(att.getNodeValue()); break;
                            case "blendMode": info.blendMode = att.getNodeValue(); break;
                            case "opacity": info.opacity = Integer.parseInt(att.getNodeValue()); if(Settings.IGNORE_OPACITY_ZERO && info.opacity==0) info.ignore = true; break;
                            case "clipping": info.clipping = att.getNodeValue(); break;
                            case "flags": info.flags = att.getNodeValue(); break;
                            case "pixelDataIrrelevant": info.pixelDataIrrelevant = Boolean.parseBoolean(att.getNodeValue()); break;
                        }
                    }
                }
                info.calc();
            }
            
            for (int j = 0; j < list.getLength(); j++) {
                var item = list.item(j);
                if(item.hasChildNodes())
                    _findLayerInfo(item.getChildNodes(), ret, limit);
            }
        }
        return ret;
    }
    
    public static String findInTree(Node list, String path, String attribute) {
        return _findInTree(list.getChildNodes(),path,attribute,new StringBuilder());
    }


    private static String _findInTree(NodeList list, String path, String attribute, StringBuilder currentPath) {
        for (int i = 0; i < list.getLength(); i++) {
            var node = list.item(i);

            if(currentPath.length()!=0)
                currentPath.append(".");

            currentPath.append(node.getNodeName());

            if(currentPath.equals("path")) {
                if (node.hasAttributes()) {
                    var ats = node.getAttributes();
                    for (int j = 0; j < ats.getLength(); j++) {
                        var att = ats.item(j);

                        if (att.getNodeName().equals(attribute))
                            return att.getNodeValue();
                    }
                }
            } else {
                for (int j = 0; j < list.getLength(); j++) {
                    var item = list.item(j);
                    if(item.hasChildNodes())
                        _findInTree(item.getChildNodes(), path, attribute, currentPath);
                }
            }
        }
        return null;
    }

    public static void iteratePrint(NodeList list) {
        StringBuilder sb = new StringBuilder();
        iteratePrint(list, sb, "");

        System.out.println(sb);
    }

    public static void iteratePrint(NodeList list, StringBuilder str, String prefix) {
        for (int i = 0; i < list.getLength(); i++) {
            var node = list.item(i);
            str.append(prefix).append(node.getNodeName());
            if(node.getNodeValue() != null)
                str.append(" = ").append(node.getNodeValue());
            if(node.hasAttributes()) {
                str.append("[");
                var ats = node.getAttributes();
                for (int j = 0; j < ats.getLength(); j++) {
                    var att = ats.item(j);
                    if(j!=0) str.append(", ");
                    str.append(att.getNodeName());
                    if(att.getNodeValue() != null)
                        str.append(" = ").append(att.getNodeValue());
                }
                str.append("]");
            }
            str.append("\n");
            iteratePrint(node.getChildNodes(), str, prefix + "    ");
            str.append(prefix);
        }
    }

    public static void writeImage(String fileName, RenderedImage image) throws IOException {
        String format = "png";
        File file = new File(fileName);

        // Get the writer
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);

        if (!writers.hasNext()) {
            throw new IllegalArgumentException("No writer for: " + format);
        }

        ImageWriter writer = writers.next();

        try {
            // Create output stream (in try-with-resource block to avoid leaks)
            try (ImageOutputStream output = ImageIO.createImageOutputStream(file)) {
                writer.setOutput(output);

                // Optionally, listen to progress, warnings, etc.

                ImageWriteParam param = writer.getDefaultWriteParam();

                // Optionally, control format specific settings of param (requires casting), or
                // control generic write settings like sub sampling, source region, output type etc.

                // Optionally, provide thumbnails and image/stream metadata
                writer.write(image);
//                writer.write(..., new IIOImage(..., image, ...), param);
            }
        }
        finally {
            // Dispose writer in finally block to avoid memory leaks
            writer.dispose();
        }
    }
}
