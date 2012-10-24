









46
//Dropbox key of exartisan@gmx.com account
	final static public String APP_KEY = "0mzyefcz6hvdowv"; //"sssv7bmbf3k8nc4";
    final static public String APP_SECRET = "bxs7zurc5n7vpeq"; //"nbakeicx5s5jhow";
    


// Prepare image for recognition
        	// rotate clockwise
        	Mat nci_tmp;
        	transpose(number_code_image,nci_tmp);
        	flip(nci_tmp,nci_tmp,1);
        	//save for debugging....
        	int alpha = 2, beta = -20;
        	for( int y = 0; y < temp.rows; y++ )
        	for( int x = 0; x < temp.cols; x++ )
        	{
        		nci_tmp.at<uchar>(y,x) = saturate_cast<uchar>( alpha*( nci_tmp.at<uchar>(y,x) ) + beta );

        	}
        	imwrite("/sdcard/aa/number_code_bef.jpg",nci_tmp);
       		// resize for recognitin
       		Mat nci(3*nci_tmp.rows, 3*nci_tmp.cols, CV_8UC1);
       		resize(nci_tmp,nci,nci.size());
       		char file[255];
       		sprintf(file, "/sdcard/aa/number_code%d.png",pre_image_for_recog_counter++);
       		imwrite(file,nci);
       		// finish prepare image




// not transform
int exam_paper_dectect(Mat gray_img, Mat color_img)
{
	int detect = 0;
	int ratio = 3;

//    Mat pyr;
//    pyrDown(gray_img, pyr, Size(gray_img.cols/ratio, gray_img.rows/ratio));
//    Mat _process_img(pyr);
//    GaussianBlur(_process_img,_process_img, Size(5,5), 10, 10);
//    threshold(_process_img,_process_img,100,255,THRESH_OTSU);//THRESH_BINARY);

    Mat pyr(gray_img.rows/ratio, gray_img.cols/ratio, CV_8UC1);
    resize(gray_img,pyr,pyr.size());
    Mat _process_img(pyr);
    Canny(_process_img, _process_img, 20, 100, 5);
//    dilate(_process_img, _process_img, Mat(), Point(-1,-1));

    vector<vector<Point> > squares;
    vector<vector<Point> > contours;
    findContours(_process_img, contours, CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);
    squares = find_rectangle(contours,2000.0,(_process_img.rows-5)*(_process_img.cols-5),0.25);

//    cvtColor(_process_img, color_img, CV_GRAY2RGB,4); // for debugging

    vector<Point> obj_max;
    int index_max_obj=0;
    double area_max_obj = 0.0;

    // find max object
    for( size_t i = 0; i < squares.size(); i++ )
    {
    	double _area =  fabs(contourArea(Mat(squares[i])));
    	if (area_max_obj<_area)
    	{
    		area_max_obj = _area;
    	    obj_max = squares[i];
    	    index_max_obj = i;
    	}
    }

    int margin = 3;
    if (obj_max.size()==4)
    {
    	paper_flag = true;
        // if down-scale image for fast
        for(int i=0;i<4;i++)
        {
        	obj_max[i]=ratio*obj_max[i];
        	squares[index_max_obj][i] = ratio*squares[index_max_obj][i];
        }
        //arcLength

    	// draw  red color boder of paper
		const Point* p1 = &obj_max[0];
		int n1 = obj_max.size();
    	polylines(color_img, &p1, &n1, 1, true, Scalar(0,0,255,255), 3, CV_AA);

    	Rect rect = boundingRect(obj_max);
    	if(((rect.x+rect.width)<gray_img.cols-margin)&&((rect.y+rect.height)<gray_img.rows-margin)&&
    			rect.x>margin&&rect.y>margin && MAX(rect.height,rect.width)>MIN(gray_img.rows,gray_img.cols))
    	{
    		paper_inside_flag = true;
     		detect = 1;

 //    		transform_image(color_img,obj_max);
 //    		rect.height = rect.width*210/297; // keep A4
 //    		rectangle(color_img,rect,Scalar(0,255,255,255),2);
     		
     		
 //    		cvtColor(color_img,gray_img,CV_RGB2GRAY);


    		// if detected exam paper then detect black stone position
    		// create image mask
    		Mat mask(gray_img.rows, gray_img.cols, CV_8UC1);
    		rectangle(mask,Rect(0,0,gray_img.cols,gray_img.rows),Scalar(0,0,0,0),CV_FILLED);
        	drawContours(mask,squares,index_max_obj,Scalar(255,255,255,255),CV_FILLED);
        	// create temp image
        	Mat temp(gray_img.rows, gray_img.cols, CV_8UC1);
        	rectangle(temp,Rect(0,0,gray_img.cols,gray_img.rows),Scalar(255,255,255,255),CV_FILLED);
        	gray_img.copyTo(temp, mask);

        	Mat answer_reg_image(gray_img.rows, gray_img.cols, CV_8UC1);
        	temp.copyTo(answer_reg_image);

        	// find three black stone
        	rectangle(temp,Rect(rect.x,rect.y+rect.height/5,rect.width,3*rect.height/5),Scalar(255,255,255,255),CV_FILLED);
        	if (find_black_stone(temp,color_img)) detect = 1;
        	else detect = 0;

         	// answer number code	45/500
        	Rect number_code_reg(rect.x+0.11*rect.width,rect.y+0.23*rect.height, 0.055*rect.width,0.45*rect.height);
        	Mat number_code_image = answer_reg_image(number_code_reg);
        	imwrite("/sdcard/aa/number_code.jpg",number_code_image);
        	pre_image_for_recog(number_code_image);
        	rectangle(/*answer_reg_image*/color_img,number_code_reg,Scalar(255,255,255,255),2);


        	//if (detect)	//NumberCodeRecog(nci,knn);// AnalyseImage(knn);

        	// answer region
        	rectangle(/*answer_reg_image*/color_img,Rect(rect.x+rect.width/5,rect.y+0.03*rect.height,4*rect.width/5-0.03*rect.height,
        			rect.height-0.06*rect.height),Scalar(255,255,255,255),2);
//        	cvtColor(answer_reg_image, color_img, CV_GRAY2RGB,4); // for debugging


//        	transform_image(color_img,obj_max);
//        	imwrite("/sdcard/pic3.jpg",color_img);

//    		slope_camera(color_img,obj_max);

        	// draw  yellow color rectangle boder of paper
//        	rectangle(color_img,rect,Scalar(0,255,255,255),2);
    	}
    	else
    	{
    		paper_inside_flag = false;
    	}
//	    	mgray.copyTo(grayimage);
//	    	contrast = contrast_measure(grayimage);

    }

/*
    for( size_t i = 0; i < squares.size(); i++ )
    {
    	const Point* p = &squares[i][0];
        int n = (int)squares[i].size();
        polylines(color_img, &p, &n, 1, true, Scalar(0,0,255,255), 3, CV_AA);
    }
*/
    //erode(grayimage,grayimage,kernel,Point(-1,-1),5);
    //dilate(grayimage,grayimage,kernel,Point(-1,-1),2);

    return detect;

}
