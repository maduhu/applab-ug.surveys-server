import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.*;

import configuration.applabConfig;

import java.util.*;

public class submission extends HttpServlet{

	private static final long serialVersionUID = 1L;
	final String DATE_FORMAT_NOW="yyyy-MM-dd HH:mm:ss";

	protected void doPost(HttpServletRequest request,HttpServletResponse response) throws ServletException,IOException
	{
		response.setContentType("text/html");
		try
		{
			if(ServletFileUpload.isMultipartContent(request))
			{
				HttpSession session=request.getSession(true);
				String server_entry_time=applabConfig.getDateTime();
				ServletFileUpload servletFileUpload=new ServletFileUpload(new DiskFileItemFactory());
				List<?> fileList=servletFileUpload.parseRequest(request);
				Iterator<?> it=fileList.iterator();
				//store the paths to the attachments in attachment path
				Hashtable<String,String> attachment_path=new Hashtable<String,String>();
				boolean has_attachments=false;
				if(fileList.size()>1)
				{
					has_attachments=true;
				}
				String appendtoFile="";
				while(it.hasNext())
				{
					FileItem fileItem=(FileItem)it.next();
					String contentType=fileItem.getContentType();
					if(contentType.equals("text/xml"))
					{
						File temp=File.createTempFile("xmlFile", ".xml");
						fileItem.write(temp);
						DocumentBuilder builder=DocumentBuilderFactory.newInstance().newDocumentBuilder();
						Document doc=builder.parse(temp);
						doc.getDocumentElement().normalize();
						//get the root node
						Element rootNode=(Element)doc.getDocumentElement();
						NodeList childNodes=rootNode.getChildNodes();
						Hashtable<String, String> surveyValues=new Hashtable<String, String>();
						//place all in the surveyValues
						surveyValues.put("server_entry_time", server_entry_time);
						for(int i=0;i<childNodes.getLength();i++)
						{
							Element element=(Element)childNodes.item(i);
							NodeList eList=element.getChildNodes();
							String elementName=element.getNodeName();
							String elementValue="";
							try
							{
								if(has_attachments)
								{
									Enumeration<String> at_keys=attachment_path.keys();
									int cursor=0;
									while(at_keys.hasMoreElements())
									{
										String at_key=at_keys.nextElement();
										if(at_key.equals(eList.item(0).getNodeValue()))
										{
											elementValue=attachment_path.get(at_key);
											cursor++;
										}
									}
									if(cursor==0)
									{
										elementValue=eList.item(0).getNodeValue().toString();
									}									
								}
								else
								{
									elementValue=eList.item(0).getNodeValue().toString();
								}
							}
							catch(NullPointerException ex)
							{
								elementValue="";
							}
							surveyValues.put(elementName, elementValue);
						}
						//get the survey_id
						String survey_id_=surveyValues.get("survey_id");
						int zbr_survey_id=Integer.parseInt(surveyValues.get("survey_id"));
						if(configuration.DbConnect.verifySurveyID(zbr_survey_id))
						{
							//The following permanent fields should not be included in creating a hex string
							String zbr_handset_submit_time=surveyValues.get("handset_submit_time");
							surveyValues.remove("handset_submit_time");
							String zbr_server_entry_time=surveyValues.get("server_entry_time");
							surveyValues.remove("server_entry_time");
							String zbr_location=surveyValues.get("location");
							//create hexstring
							Enumeration<String> _keys=surveyValues.keys();
							String zbr_hash_str="";
							while(_keys.hasMoreElements())
							{
								String _key=_keys.nextElement();
								String _value=surveyValues.get(_key);	
								zbr_hash_str=zbr_hash_str+""+_value;
							}
							String zbr_hex_hash=configuration.md5.getMD5Hash(zbr_hash_str);
							//extract the permanent fields
							String zbr_handset_id=surveyValues.get("handset_id");
							surveyValues.remove("handset_id");
							String zbr_interviewer_id=surveyValues.get("interviewer_id");
							surveyValues.remove("interviewer_id");
							surveyValues.remove("survey_id");
							
							Enumeration<String> keys=surveyValues.keys();
							int cursor=0;
							String tb_fields="";
							String tb_field_value="";
							while(keys.hasMoreElements())
							{
								String key=keys.nextElement();
								String value=surveyValues.get(key);
								//verify that these questions have been created
								if(configuration.DbConnect.verifySurveyField(key, zbr_survey_id))
								{
									if(tb_fields.equals(""))
									{
										tb_fields=key+""+tb_fields;
									}
									else
									{
										tb_fields=tb_fields+","+key;
									}
									if(tb_field_value.equals(""))
									{
										tb_field_value="'"+value+"'"+tb_field_value;
									}
									else
									{
										tb_field_value=tb_field_value+",'"+value+"'";
									}
									cursor++;
								}
							}
							if(cursor>0)
							{
								String query_str="insert into zebrasurveysubmissions (survey_id,server_entry_time,handset_submit_time,handset_id,interviewee_name,result_hash,location,"+tb_fields+") values ("+zbr_survey_id+",'"+zbr_server_entry_time+"','"+zbr_handset_submit_time+"','"+zbr_handset_id+"','"+zbr_interviewer_id+"','"+zbr_hex_hash+"','"+zbr_location+"',"+tb_field_value+")";
								String submitted_survey=configuration.DbConnect.postSubmission(query_str);
								if(submitted_survey.equals("Data Posted"))
								{
									response.setStatus(HttpServletResponse.SC_CREATED);
									response.setHeader("Location", request.getRequestURI());
								}
								else
								{
									response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
									response.setHeader("Location", request.getRequestURI());
								}
							}
							else
							{
								response.setStatus(HttpServletResponse.SC_NOT_FOUND);
								response.setHeader("Location", request.getRequestURI());
							}
						}
						else
						{
							response.setStatus(HttpServletResponse.SC_NOT_FOUND);
							response.setHeader("Location", request.getRequestURI());
						}
					}
					else if(contentType.equals("image/jpeg") || contentType.equals("image/gif") || contentType.equals("image/png") || contentType.equals("image/bmp"))
					{
						String image_name="";
						configuration.images img=new configuration.images();
						if(contentType.equals("image/jpeg"))
						{
							image_name=img.generateImageName()+".jpg";
						}
						else if(contentType.equals("image/gif"))
						{
							image_name=img.generateImageName()+".gif";
						}
						else if(contentType.equals("image/png"))
						{
							image_name=img.generateImageName()+".png";
						}
						else if(contentType.equals("image/bmp"))
						{
							image_name=img.generateImageName()+".bmp";
						}
						if(!image_name.equals(""))
						{
							//save this image
							String dir=getServletContext().getRealPath("/applabimages/"+image_name);
							File file=new File(dir);
							fileItem.write(file);
							attachment_path.put(fileItem.getName(), "/applabimages/"+image_name);
						}
					}
					else if(contentType.equals("video/3gp") || contentType.equals("video/mp4") || contentType.equals("video/3gpp"))
					{
						String video_name="";
						configuration.images mdi=new configuration.images();
						if(contentType.equals("video/3gp"))
						{
							video_name=mdi.generateImageName()+".3gp";
						}
						else if(contentType.equals("video/mp4"))
						{
							video_name=mdi.generateImageName()+".mp4";
						}
						else if(contentType.equals("video/3gpp"))
						{
							video_name=mdi.generateImageName()+".3gp";
						}
						if(!video_name.equals(""))
						{
							String dir=getServletContext().getRealPath("/videos/"+video_name);
							File file=new File(dir);
							fileItem.write(file);
							attachment_path.put(fileItem.getName(), "/videos/"+video_name);
						}
					}
					else if(contentType.equals("audio/3gp") || contentType.equals("audio/mp4") || contentType.equals("audio/m4a") || contentType.equals("audio/3gpp"))
					{
						String audio_name="";
						configuration.images mdi=new configuration.images();
						if(contentType.equals("audio/3gpp"))
						{
							audio_name=mdi.generateImageName()+".3gpp";
						}
						if(!audio_name.equals(""))
						{
							String dir=getServletContext().getRealPath("/audios/"+audio_name);
							File file=new File(dir);
							fileItem.write(file);
							attachment_path.put(fileItem.getName(), "/audios/"+audio_name);
						}
					}
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();

			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setHeader("Location", request.getRequestURI());
		}
	}
}
