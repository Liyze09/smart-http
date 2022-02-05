/*******************************************************************************
 * Copyright (c) 2017-2022, org.smartboot. All rights reserved.
 * project name: smart-http
 * file name: BitTree.java
 * Date: 2022-01-02
 * Author: sandao (zhengjunweimail@163.com)
 ******************************************************************************/

package org.smartboot.http.common.utils;

/**
 * @author 三刀（zhengjunweimail@163.com）
 * @version V1.0 , 2022/1/2
 */
public class ByteTree<T> {
    private final byte value;
    private final int depth;
    private final ByteTree<T> parent;
    private int shift = -1;
    private ByteTree<T>[] nodes;
    private String stringValue;
    /**
     * 捆绑附件对象
     */
    private T attach;

    public ByteTree() {
        this(null, Byte.MIN_VALUE);
    }

    public ByteTree(ByteTree<T> parent, byte value) {
        this.parent = parent;
        this.value = value;
        this.depth = parent == null ? 0 : parent.depth + 1;
    }

    public int getDepth() {
        return depth;
    }

    public ByteTree<T> search(byte[] bytes, int offset, int len, byte[] ends) {
        return search(bytes, offset, len, ends, true);
    }

    private ByteTree<T> clone(ByteTree<T> byteTree) {
        byte[] b = new byte[byteTree.getDepth()];
        ByteTree<T> tree = byteTree;
        while (tree.depth != 0) {
            b[tree.depth - 1] = tree.value;
            tree = tree.parent;
        }
        ByteTree<T> clone = new ByteTree<T>(null, Byte.MAX_VALUE);
        clone.addNode(b, 0, b.length);
        return clone;
    }

    /**
     * 从给定的字节数组总匹配出特定结尾的区块
     *
     * @param bytes  待匹配的字节数组
     * @param offset 起始位置
     * @param limit  截止位置
     * @param ends   结束符
     * @param cache  是否缓存新节点
     * @return
     */
    public ByteTree<T> search(byte[] bytes, int offset, int limit, byte[] ends, boolean cache) {
        ByteTree<T> byteTree = this;
        while (true) {
            if (offset >= limit) {
                return null;
            }
            for (byte end : ends) {
                if (end == bytes[offset]) {
                    return byteTree;
                }
            }

            int i = bytes[offset] - byteTree.shift;
            if (byteTree.nodes == null || i >= byteTree.nodes.length || i < 0) {
                break;
            }
            ByteTree<T> b = byteTree.nodes[i];
            if (b != null) {
                byteTree = b;
                offset++;
//                return b.search(bytes, offset + 1, limit, ends, cache);
            } else {
                break;
            }
        }
        if (cache) {
            //在当前节点上追加子节点
//            System.out.println("add");
            byteTree.addNode(bytes, offset, limit);
            return byteTree.search(bytes, offset, limit, ends, cache);
        } else {
//            System.out.println("tmp");
            // 构建临时对象，用完由JVM回收
            ByteTree<T> clone = clone(byteTree);
            return clone.search(bytes, offset - byteTree.depth, limit, ends, true);
        }
    }

    public void addNode(String value, T attach) {
        byte[] bytes = value.getBytes();
        ByteTree<T> tree = this;
        while (tree.depth > 0) {
            tree = tree.parent;
        }
        ByteTree<T> leafNode = tree.addNode(bytes, 0, bytes.length);
        leafNode.stringValue = value;
        leafNode.attach = attach;
    }

    /**
     * 从根节点开始，为入参字符串创建节点
     */
    public void addNode(String value) {
        addNode(value, null);
    }

    private ByteTree<T> addNode(byte[] value, int offset, int limit) {
        if (offset == limit) {
            return this;
        }

        byte b = value[offset];
        if (shift == -1) {
            shift = b;
        }
        if (b - shift < 0) {
            increase(b - shift);
        } else {
            increase(b + 1 - shift);
        }

        ByteTree<T> nextTree = nodes[b - shift];
        if (nextTree == null) {
            nextTree = nodes[b - shift] = new ByteTree<T>(this, b);
        }
        return nextTree.addNode(value, offset + 1, limit);
    }

    private void increase(int size) {
        if (size == 0)
            size = -1;
        if (nodes == null) {
            nodes = new ByteTree[size];
        } else if (size < 0) {
            ByteTree<T>[] temp = new ByteTree[nodes.length - size];
            System.arraycopy(nodes, 0, temp, -size, nodes.length);
            nodes = temp;
            shift += size;
        } else if (nodes.length < size) {
            ByteTree<T>[] temp = new ByteTree[size];
            System.arraycopy(nodes, 0, temp, 0, nodes.length);
            nodes = temp;
        }
    }

    public String getStringValue() {
        if (stringValue == null) {
            byte[] b = new byte[depth];
            ByteTree<T> tree = this;
            while (tree.depth != 0) {
                b[tree.depth - 1] = tree.value;
                tree = tree.parent;
            }
            stringValue = new String(b);
        }
        return stringValue;
    }

    public T getAttach() {
        return attach;
    }
}
