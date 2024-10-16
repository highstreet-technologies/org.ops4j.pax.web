<%--

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.

--%>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="java.util.Date" %>

<!DOCTYPE html>
<html>
<head>
	<title>Hello JSP</title>
	<meta charset="UTF-8">
</head>
<body>
	<%
		@SuppressWarnings("UnhandledExceptionInJSP")
		Date d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2019-02-11 02:03:04");
		request.setAttribute("time", new SimpleDateFormat("HH:mm").format(d));
	%>
	<p id="p1">It's <%= request.getAttribute("time") %> o'clock</p>
</body>
</html>
