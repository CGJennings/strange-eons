/*
 imageutils.js - version 16
 Support functions for working with bitmapped images.
 */

importClass(java.awt.image.BufferedImage);

const ImageUtils = (function () {
    const valDimen = (dim) => {
        if (isNaN(dim))
            throw new TypeError("dimension is not a number: " + dim);
        if (dim < 1 || dim > 40000)
            throw new RangeError("bad image dimension: " + dim);
        return dim;
    };

    const valImage = (im) => {
        if (!(im instanceof BufferedImage))
            throw new TypeError("not an image: " + im);
        return im;
    };

    const valNum = (x) => {
        if (isNaN(x))
            throw new TypeError("coordinate is not a number: " + x);
        return x;
    };

    let ImageUtils = {};

    ImageUtils.create = function create(width, height, transparent) {
        let type = transparent ? java.awt.image.BufferedImage.TYPE_INT_ARGB
                : java.awt.image.BufferedImage.TYPE_INT_RGB;
        return new java.awt.image.BufferedImage(valDimen(width), valDimen(height), type);
    };

    ImageUtils.createForResolution = function createForResolution(ppi, width, height, transparent) {
        if (isNaN(ppi))
            throw new TypeError("resolution is not a number" + ppi);
        if (ppi <= 0)
            throw new RangeError("bad resolution " + ppi);
        return ImageUtils.create(width / 72 * ppi, height / 72 * ppi, transparent);
    };

    ImageUtils.get = function get(resPath, cacheResult, quietly) {
        let im = null;
        if (cacheResult || cacheResult === undefined) {
            if (quietly) {
                im = resources.ResourceKit.getImageQuietly(resPath);
            } else {
                im = resources.ResourceKit.getImage(resPath);
            }
        } else {
            let url = resources.ResourceKit.composeResourceURL(resPath);
            if (url)
                im = javax.imageio.ImageIO.read(url);
            if (im)
                im = resources.ResourceKit.prepareNewImage(im);
            if (im === null && !quietly) {
                arkham.dialog.ErrorDialog.displayErrorOnce(
                        resource, string("rk-err-image-resource", resource), null
                        );
            }
        }
        return im;
    };
    
    ImageUtils.getMultiRes = function getMultiRes(resPath) {
        return new ca.cgjennings.graphics.MultiResolutionImageResource(resPath);
    };

    ImageUtils.getIcon = function getIcon(resPath, unthemed) {
        if (unthemed) {
            return new swing.ImageIcon(ImageUtils.getMultiRes(resPath));
        } else {
            let icon = new ca.cgjennings.ui.theme.ThemedImageIcon(resPath);
            return icon.image === null ? null : icon;
        }
    };

    ImageUtils.createIcon = function createIcon(image, size) {
        if (isNaN(size) || size < 1)
            size = 18;
        return ca.cgjennings.graphics.ImageUtilities.createIconForSize(valImage(image), size);
    };


    /**
     * ImageUtils.copy( image )
     * Returns a new copy of <tt>image</tt>. If you are going to draw on an image
     * that you obtained from resources, it is important to work with a copy
     * or you will corrupt the shared version stored in the image cache.
     *
     * returns a copy of the source image
     */
    ImageUtils.copy = function copy(image) {
        return ca.cgjennings.graphics.ImageUtilities.copy(valImage(image));
    };

    ImageUtils.STITCH_HORIZONTAL = 1;
    ImageUtils.STITCH_VERTICAL = 2;
    ImageUtils.stitch = function stitch(image1, image2, stitchEdge) {
        valImage(image1);
        valImage(image2);
        if (stitchEdge !== ImageUtils.STITCH_HORIZONTAL && stitchEdge !== ImageUtils.STITCH_VERTICAL) {
            throw new TypeError("invalid stitchEdge: " + stitchEdge);
        }

        let OPAQUE = java.awt.Transparency.OPAQUE;
        let transparent = true;
        if (image1.getTransparency() === OPAQUE && image2.getTransparency() === OPAQUE) {
            transparent = false;
        }

        let im, g, w, h;
        try {
            if (stitchEdge === ImageUtils.STITCH_HORIZONTAL) {
                w = image1.getWidth() + image2.getWidth();
                h = Math.max(image1.getHeight(), image2.getHeight());
                im = ImageUtils.create(w, h, transparent);
            } else {
                w = Math.max(image1.getWidth(), image2.getWidth());
                h = image1.getHeight() + image2.getHeight();
                im = ImageUtils.create(w, h, transparent);
            }

            g = im.createGraphics();
            g.drawImage(image1, 0, 0, null);

            if (stitchEdge === ImageUtils.STITCH_HORIZONTAL) {
                g.drawImage(image2, image1.getWidth(), 0, null);
            } else {
                g.drawImage(image2, 0, image1.getHeight(), null);
            }
        } finally {
            if (g)
                g.dispose();
        }
        return im;
    };

    ImageUtils.resize = function resize(image, width, height, fast) {
        if (fast) {
            return ca.cgjennings.graphics.ImageUtilities.resample(
                    valImage(image), valDimen(width), valDimen(height), false,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR, null
                    );
        } else {
            return ca.cgjennings.graphics.ImageUtilities.resample(valImage(image), valDimen(width), valDimen(height));
        }
    };

    ImageUtils.fit = function fit(image, width, height, fast) {
        valImage(image);
        valDimen(width);
        valDimen(height);
        if (image.width === width && image.height <= height) {
            return image;
        }
        if (image.height === height && image.width <= width) {
            return image;
        }
        let scale = Math.min(width / image.width, height / image.height);
        return ImageUtils.resize(
                image, Math.round(image.width * scale), Math.round(image.height * scale), fast
                );
    };

    ImageUtils.crop = function crop(image, x, y, width, height) {
        valImage(image);
        valNum(x);
        valNum(y);

        if (width == null || width < 1)
            width = image.width - x;
        if (height == null || height < 1)
            height = image.height - y;
        valDimen(width);
        valDimen(height);

        let dest = ca.cgjennings.graphics.ImageUtilities.createCompatibleIntRGBFormat(image, width, height);
        let g = dest.createGraphics();
        try {
            g.drawImage(image, -x, -y, null);
        } finally {
            g.dispose();
        }
        return dest;
    };

    ImageUtils.pad = function pad(image, top, left, bottom, right) {
        if (left == null) {
            left = bottom = right = top;
        }
        return ca.cgjennings.graphics.ImageUtilities.pad(
                valImage(image),
                valNum(top), valNum(left), valNum(bottom), valNum(right)
                );
    };

    ImageUtils.tint = function tint(image, h, s, b) {
        if (arguments.length === 2) {
            let hsb = Array.from(h);
            h = hsb[0];
            s = hsb[1];
            b = hsb[2];
        } else if (arguments.length !== 4) {
            throw new Error("missing (h, s, b)");
        }

        let hsbFilter = new ca.cgjennings.graphics.filters.TintFilter(h, s, b);
        image = ca.cgjennings.graphics.ImageUtilities.ensureIntRGBFormat(valImage(image));
        return hsbFilter.filter(image, null);
    };

    ImageUtils.mirror = function mirror(image, horiz, vert) {
        return ca.cgjennings.graphics.ImageUtilities.flip(
                valImage(image),
                horiz == null ? true : horiz,
                vert == null ? false : vert
                );
    };

    ImageUtils.invert = function invert(image) {
        return ca.cgjennings.graphics.ImageUtilities.invert(valImage(image));
    };

    ImageUtils.desaturate = function desaturate(image) {
        return ca.cgjennings.graphics.ImageUtilities.desaturate(valImage(image));
    };

    ImageUtils.trim = function trim(image) {
        return ca.cgjennings.graphics.ImageUtilities.trim(valImage(image));
    };

    ImageUtils.read = function read(file) {
        if (file == null)
            throw new Error("missing file");
        if (!(file instanceof java.io.File)) {
            file = new java.io.File(String(file));
        }
        let image = javax.imageio.ImageIO.read(file);
        if (image == null)
            throw new Error("unable to read file");
        return resources.ResourceKit.prepareNewImage(image);
    };

    ImageUtils.write = function write(image, file, format, quality, progressive, ppi) {
        if (file == null)
            throw new Error("missing file");
        if (format == null)
            format = ImageUtils.FORMAT_PNG;
        if (quality == null)
            quality = 0.75;
        if (progressive == null)
            progressive = false;
        if (!(file instanceof java.io.File)) {
            file = new java.io.File(String(file));
        }

        let iw;
        try {
            iw = new ca.cgjennings.imageio.SimpleImageWriter(format);
            if (ppi === undefined) {
                iw.metadataEnabled = false;
            } else {
                iw.pixelsPerInch = ppi;
            }
            iw.compressionQuality = quality;
            iw.progressiveScan = progressive;
            iw.write(valImage(image), file);
        } finally {
            iw.dispose();
        }
    };

    ImageUtils.FORMAT_PNG = ca.cgjennings.imageio.SimpleImageWriter.FORMAT_PNG;
    ImageUtils.FORMAT_JPEG = ca.cgjennings.imageio.SimpleImageWriter.FORMAT_JPEG;
    ImageUtils.FORMAT_JPEG2000 = ca.cgjennings.imageio.SimpleImageWriter.FORMAT_JPEG2000;
    ImageUtils.FORMAT_BMP = ca.cgjennings.imageio.SimpleImageWriter.FORMAT_BMP;
    ImageUtils.FORMAT_GIF = ca.cgjennings.imageio.SimpleImageWriter.FORMAT_GIF;

    ImageUtils.save = function save(image, defaultFile, parent) {
        valImage(image);
        if (defaultFile == null) {
            defaultFile = Settings.shared.get("default-image-folder");
        }
        if (defaultFile != null && !(defaultFile instanceof java.io.File)) {
            defaultFile = new java.io.File(String(defaultFile));
        }
        if (parent === undefined)
            parent = Eons.window;

        let fc = arkham.dialog.ImageViewer.getFileChooser();
        fc.selectedFile = defaultFile;

        if (fc.showSaveDialog(parent) != swing.JFileChooser.APPROVE_OPTION) {
            return null;
        }

        let type;
        let file = fc.selectedFile;
        let ext = arkham.project.ProjectUtilities.getFileExtension(file);
        switch (ext) {
            case ImageUtils.FORMAT_JPEG:
            case ImageUtils.FORMAT_JPEG2000:
            case ImageUtils.FORMAT_GIF:
            case ImageUtils.FORMAT_BMP:
                type = ext;
                break;
            default:
                type = ImageUtils.FORMAT_PNG;
        }
        if (!ext.isEmpty()) {
            type = ext;
        }

        ImageUtils.write(image, file, type);

        // update default save location
        parent = file.parentFile;
        if (parent != null) {
            Settings.user.set("default-image-folder", parent.absolutePath);
        }

        return file;
    };

    ImageUtils.view = function view(image, title, modal, parent) {
        if (modal === undefined)
            modal = false;
        if (parent === undefined)
            parent = Eons.window;
        let d = new arkham.dialog.ImageViewer(parent, valImage(image), modal);
        if (title != null)
            d.setTitle(title);
        d.setLocationByPlatform(true);
        d.setVisible(true);
    };

    return ImageUtils;
})();