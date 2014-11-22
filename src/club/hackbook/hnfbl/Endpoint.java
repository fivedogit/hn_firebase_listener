package club.hackbook.hnfbl;


import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class Endpoint extends HttpServlet {

	// static variables:
	private static final long serialVersionUID = 1L;


	public void init(ServletConfig servlet_config) throws ServletException {
		super.init(servlet_config);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json; charset=UTF-8;");
		response.setHeader("Access-Control-Allow-Origin", "*"); // FIXME
		PrintWriter out = response.getWriter();
		out.println("{ \"response_status\": \"error\", \"message\": \"This endpoint doesn't speak POST\"}");
		return;
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		//System.out.println("endpoint.doGet(): entering...");
		response.setContentType("application/json; charset=UTF-8;");
		response.setHeader("Access-Control-Allow-Origin","*"); //FIXME
		PrintWriter out = response.getWriter();
		out.println("{ \"response_status\": \"error\", \"message\": \"This endpoint doesn't speak GET\"}");
		return; 	
	}

}
