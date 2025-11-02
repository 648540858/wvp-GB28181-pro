package com.genersoft.iot.vmp.utils;

import com.genersoft.iot.vmp.gb28181.controller.bean.Extent;

import java.util.ArrayList;
import java.util.List;

public class TileUtils {
    private static final double MAX_LATITUDE = 85.05112878;

    /**
     * 根据坐标获取指定层级的x y 值
     *
     * @param lon 经度
     * @param lat 纬度
     * @param z   层级
     * @return double[2] = {xTileFloat, yTileFloat}
     */
    public static double[] lonLatToTileXY(double lon, double lat, int z) {
        double n = Math.pow(2.0, z);
        double x = (lon + 180.0) / 360.0 * n;

        // clamp latitude to WebMercator bounds
        double latClamped = Math.max(Math.min(lat, MAX_LATITUDE), -MAX_LATITUDE);
        double latRad = Math.toRadians(latClamped);
        double y = (1.0 - (Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI)) / 2.0 * n;

        return new double[]{x, y};
    }

    /**
     * 根据坐标范围获取指定层级的x y 范围
     *
     * @param bbox array length 4: {minLon, minLat, maxLon, maxLat}
     * @param z    zoom level
     * @return TileRange object with xMin,xMax,yMin,yMax,z
     */
    public static TileRange bboxToTileRange(double[] bbox, int z) {
        double minLon = bbox[0], minLat = bbox[1], maxLon = bbox[2], maxLat = bbox[3];

        // If bbox crosses antimeridian (minLon > maxLon), caller should split into two bboxes.
        if (minLon > maxLon) {
            throw new IllegalArgumentException("bbox crosses antimeridian; split it before calling bboxToTileRange.");
        }

        double[] tMin = lonLatToTileXY(minLon, maxLat, z); // top-left (use maxLat)
        double[] tMax = lonLatToTileXY(maxLon, minLat, z); // bottom-right (use minLat)

        int xMin = (int) Math.floor(Math.min(tMin[0], tMax[0]));
        int xMax = (int) Math.floor(Math.max(tMin[0], tMax[0]));
        int yMin = (int) Math.floor(Math.min(tMin[1], tMax[1]));
        int yMax = (int) Math.floor(Math.max(tMin[1], tMax[1]));

        int maxIndex = ((int) Math.pow(2, z)) - 1;
        xMin = clamp(xMin, 0, maxIndex);
        xMax = clamp(xMax, 0, maxIndex);
        yMin = clamp(yMin, 0, maxIndex);
        yMax = clamp(yMax, 0, maxIndex);

        return new TileRange(xMin, xMax, yMin, yMax, z);
    }

    /**
     * If bbox crosses antimeridian (minLon > maxLon), split into two bboxes:
     * [minLon, minLat, 180, maxLat] and [-180, minLat, maxLon, maxLat]
     *
     * @param bbox input bbox array length 4
     * @return list of 1 or 2 bboxes (each double[4])
     */
    public static List<double[]> splitAntimeridian(double[] bbox) {
        double minLon = bbox[0], minLat = bbox[1], maxLon = bbox[2], maxLat = bbox[3];
        List<double[]> parts = new ArrayList<>();
        if (minLon <= maxLon) {
            parts.add(new double[]{minLon, minLat, maxLon, maxLat});
        } else {
            parts.add(new double[]{minLon, minLat, 180.0, maxLat});
            parts.add(new double[]{-180.0, minLat, maxLon, maxLat});
        }
        return parts;
    }

    private static int clamp(int v, int a, int b) {
        return Math.max(a, Math.min(b, v));
    }

    /**
     * Return list of tile coordinates (x,y,z) covering bbox at zoom z.
     * Be careful: the number of tiles can be large for high zooms & big bbox.
     *
     */
    public static List<TileCoord> tilesForBoxAtZoom(Extent extent, int z) {
        List<TileCoord> tiles = new ArrayList<>();
        List<double[]> parts = splitAntimeridian(new double[]{extent.getMinLng(), extent.getMinLat(),
                extent.getMaxLng(), extent.getMaxLat()});
        for (double[] part : parts) {
            TileRange range = bboxToTileRange(part, z);
            for (int x = range.xMin; x <= range.xMax; x++) {
                for (int y = range.yMin; y <= range.yMax; y++) {
                    tiles.add(new TileCoord(x, y, z));
                }
            }
        }
        return tiles;
    }

    // Simple helper classes
    public static class TileRange {
        public final int xMin, xMax, yMin, yMax, z;
        public TileRange(int xMin, int xMax, int yMin, int yMax, int z) {
            this.xMin = xMin; this.xMax = xMax; this.yMin = yMin; this.yMax = yMax; this.z = z;
        }
    }

    public static class TileCoord {
        public final int x, y, z;
        public TileCoord(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
        @Override
        public String toString() {
            return "{" + "z=" + z + ", x=" + x + ", y=" + y + '}';
        }
    }
}
