/*
 * Copyright 2013 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */


import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpHeaders.Values;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.*;

public class HttpHelloWorldServerHandler extends ChannelInboundHandlerAdapter {
    private static byte[] CONTENT = null;
    private static ConcurrentHashMap hm = new ConcurrentHashMap();

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException {
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;

            if (HttpHeaders.is100ContinueExpected(req)) {
                ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE));
            }
            boolean keepAlive = HttpHeaders.isKeepAlive(req);

            FullHttpResponse response = null;
            if (req.uri().equals("/img_avatar2.png"))
            {
                CONTENT = Files.readAllBytes(Paths.get("img_avatar2.png"));
                response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(CONTENT));
                response.headers().set(CONTENT_TYPE, "image/png");
                response.headers().set(CONTENT_LENGTH, response.content().readableBytes());

            }else if (req.uri().equals("/admin")){
                boolean isAdmin = sess(req.uri(), ctx);
                if (isAdmin){
                    response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer("200 Hello, Admin!".getBytes()));
                    response.headers().set(CONTENT_TYPE, "text/plain");
                    response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
                }
                else{
                    response = new DefaultFullHttpResponse(HTTP_1_1, NOT_FOUND, Unpooled.wrappedBuffer("404 Page not found!".getBytes()));
                    response.headers().set(CONTENT_TYPE, "text/plain");
                    response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
                }
            }
            else{
                sess(req.uri(), ctx);

                if (hm.get(ctx.channel().id()) == null) { // login form
                    CONTENT = Files.readAllBytes(Paths.get("1.html"));
                    response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(CONTENT));
                    response.headers().set(CONTENT_TYPE, "text/html");
                    response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
                } else {
                    CONTENT = Files.readAllBytes(Paths.get("2.html"));
                    response = new DefaultFullHttpResponse(HTTP_1_1, OK, Unpooled.wrappedBuffer(CONTENT));
                    response.headers().set(CONTENT_TYPE, "text/html");
                    response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
                }


            }

            if (!keepAlive) {
                ctx.write(response).addListener(ChannelFutureListener.CLOSE);
            } else {
                response.headers().set(CONNECTION, Values.KEEP_ALIVE);
                ctx.write(response);
            }
        }
    }

    private boolean sess(String uri, ChannelHandlerContext ctx){
        System.out.println(uri);
        System.out.println("SessID = " + hm.get(ctx.channel().id()));

        if (uri.matches("(.*)?uname=user&psw=user(.*)")){
            hm.put(ctx.channel().id(), "user");
        }else if (uri.matches("(.*)?uname=admin&psw=admin(.*)")){
            hm.put(ctx.channel().id(), "admin");
        }else if (uri.matches("(.*)?action=logout(.*)")){
            hm.remove(ctx.channel().id());
        }
        if ((hm.get(ctx.channel().id())!=null) && (hm.get(ctx.channel().id()).equals("admin")))
            return true;
        else
            return false;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
