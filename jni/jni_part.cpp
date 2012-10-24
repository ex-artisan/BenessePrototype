#include <jni.h>
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <opencv2/opencv.hpp>
#include "opencv2/ml/ml.hpp">
#include "opencv2/highgui/highgui.hpp"

#include <iostream>
#include <stdio.h>
#include <vector>
#include <fstream>

using namespace std;
using namespace cv;

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//tesseract::TessBaseAPI api;

int size=200;
class KNNWrapper
{
//	private:
	int K;
	CvKNearest knn;

	public:



	float evaluate(cv::Mat& predicted, cv::Mat& actual)
	{
		assert(predicted.rows == actual.rows);

		int t = 0;
		int f = 0;
		for(int i = 0; i < actual.rows; i++)
		{
			float p = predicted.at<float>(i,0);
			float a = actual.at<float>(i,0);
			if((p >= 0.0 && a >= 0.0) || (p <= 0.0 &&  a <= 0.0))
			{
				t++;
			}
			else
			{
				f++;
			}
		}
		return (t * 1.0) / (t + f);
	}

	// plot data and class
	void plot_binary(cv::Mat& data, cv::Mat& classes, string name)
	{
		cv::Mat plot(size, size, CV_8UC3);
		plot.setTo(cv::Scalar(255.0,255.0,255.0));
		for(int i = 0; i < data.rows; i++) {

			float x = data.at<float>(i,0) * size;
			float y = data.at<float>(i,1) * size;

			if(classes.at<float>(i, 0) > 0) {
				cv::circle(plot, Point(x,y), 2, CV_RGB(255,0,0),1);
			} else {
				cv::circle(plot, Point(x,y), 2, CV_RGB(0,255,0),1);
			}
		}
		cv::imshow(name, plot);
	}


	void train(cv::Mat& trainingData, cv::Mat& trainingClasses, int K)
	{
		this-knn.train(trainingData, trainingClasses, cv::Mat(), false, K,false);
		this->K = K;
	}

	void test( cv::Mat& testData, cv::Mat& testClasses)
	{
		cv::Mat predicted(testClasses.rows, 1, CV_32F);

		for(int i = 0; i < testData.rows; i++)
		{
			const cv::Mat samples = testData.row(i);
			//predicted.at<float>(i,0) = this->knn.find_nearest(sample, readK());
			predicted.at<float>(i,0) = this->knn.find_nearest(samples, readK(), 0, 0, 0, 0 );

		}

//		cout << "Accuracy_{KNN} = " << evaluate(predicted, testClasses) << endl;
		plot_binary(testData, predicted, "Predictions KNN");
	}
	int readK()
	{
		return this->K;
	}
};


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// test digit recognition


/////////////////////////////////////////
#define MAX_NUM_IMAGES	60000

class DigitRecognizer
{
public:
	DigitRecognizer();
	~DigitRecognizer();

	bool train(char* trainPath, char* labelsPath);
	float classify(Mat img);

private:
	cv::Mat preprocessImage(Mat img);
	int readFlippedInteger(FILE *fp);

private:
	KNearest	*knn;
	int numRows, numCols, numImages;
};
///////////////////////////////////////////
DigitRecognizer::DigitRecognizer()
{
	knn = new KNearest();
}

DigitRecognizer::~DigitRecognizer()
{
	delete knn;
}


cv::Mat DigitRecognizer::preprocessImage(cv::Mat img)
{
	//Mat cloneImg = Mat(numRows, numCols, CV_8UC1);
	//resize(img, cloneImg, Size(numCols, numRows));

	// Try to position the given image so that the
	// character is at the center

	// Step 1: Find a good enough bounding box
	// How? Starting at the center, find the first rows and colums
	// in 4 directions such that less than 10% of the pixels are
	// bright

	int rowTop=-1, rowBottom=-1, colLeft=-1, colRight=-1;

	Mat temp;
	int thresholdBottom = 50;
	int thresholdTop = 50;
	int thresholdLeft = 50;
	int thresholdRight = 50;
	int center = img.rows/2;
	for(int i=center;i<img.rows;i++)
	{
		if(rowBottom==-1)
		{
			temp = img.row(i);
			IplImage stub = temp;
			if(cvSum(&stub).val[0] < thresholdBottom || i==img.rows-1)
				rowBottom = i;
		}

		if(rowTop==-1)
		{
			temp = img.row(img.rows-i);
			IplImage stub = temp;
			if(cvSum(&stub).val[0] < thresholdTop || i==img.rows-1)
				rowTop = img.rows-i;
		}

		if(colRight==-1)
		{
			temp = img.col(i);
			IplImage stub = temp;
			if(cvSum(&stub).val[0] < thresholdRight|| i==img.cols-1)
				colRight = i;
		}

		if(colLeft==-1)
		{
			temp = img.col(img.cols-i);
			IplImage stub = temp;
			if(cvSum(&stub).val[0] < thresholdLeft|| i==img.cols-1)
				colLeft = img.cols-i;
		}
	}

	// Point2i pt = Point((colLeft+colRight)/2, (rowTop+rowBottom)/2);
	/*line(img, Point(0, rowTop), Point(img.cols, rowTop), cvScalar(255,255,255));
	line(img, Point(0, rowBottom), Point(img.cols, rowBottom), cvScalar(255,255,255));
	line(img, Point(colLeft, 0), Point(colLeft, img.rows), cvScalar(255,255,255));
	line(img, Point(colRight, 0), Point(colRight, img.rows), cvScalar(255,255,255));

	imshow("Testing the image", img);
	cvWaitKey(0);*/

	// Now, position this into the center

	Mat newImg;
	newImg = newImg.zeros(img.rows, img.cols, CV_8UC1);

	int startAtX = (newImg.cols/2)-(colRight-colLeft)/2;
	int startAtY = (newImg.rows/2)-(rowBottom-rowTop)/2;

	for(int y=startAtY;y<(newImg.rows/2)+(rowBottom-rowTop)/2;y++)
	{
		uchar *ptr = newImg.ptr<uchar>(y);
		for(int x=startAtX;x<(newImg.cols/2)+(colRight-colLeft)/2;x++)
		{
			ptr[x] = img.at<uchar>(rowTop+(y-startAtY),colLeft+(x-startAtX));
		}
	}


	Mat cloneImg = Mat(numRows, numCols, CV_8UC1);
	resize(newImg, cloneImg, Size(numCols, numRows));

	imwrite("/sdcard/digit/resize.png",cloneImg);
/*	// Now fill along the borders
	for(int i=0;i<cloneImg.rows;i++)
	{
		floodFill(cloneImg, cvPoint(0, i), cvScalar(0,0,0));
		floodFill(cloneImg, cvPoint(cloneImg.cols-1, i), cvScalar(0,0,0));

		floodFill(cloneImg, cvPoint(i, 0), cvScalar(0));
		floodFill(cloneImg, cvPoint(i, cloneImg.rows-1), cvScalar(0));
	}
*/
	imwrite("/sdcard/digit/after_resize.png",cloneImg);
//	imshow("testing image", afresize);

	cloneImg = cloneImg.reshape(1, 1);

	return cloneImg;
}
float DigitRecognizer::classify(cv::Mat img)
{
	imwrite("/sdcard/digit/classify.png",img);
//	Mat cloneImg = preprocessImage(img);

//	return knn->find_nearest(Mat_<float>(cloneImg), 1);
	return knn->find_nearest(img, 1);
}

int DigitRecognizer::readFlippedInteger(FILE *fp)
{
	int ret = 0;
	jbyte *temp;

	temp = (jbyte*)(&ret);
	fread(&temp[3], sizeof(jbyte), 1, fp);
	fread(&temp[2], sizeof(jbyte), 1, fp);
	fread(&temp[1], sizeof(jbyte), 1, fp);
	fread(&temp[0], sizeof(jbyte), 1, fp);

	return ret;
}

bool DigitRecognizer::train(char *trainPath, char *labelsPath)
{
	FILE *fp = fopen(trainPath, "rb");
	FILE *fp2 = fopen(labelsPath, "rb");

	if(!fp || !fp2)
		return false;

	// Read bytes in flipped order
	int magicNumber = readFlippedInteger(fp);
	numImages = readFlippedInteger(fp);
	numRows = readFlippedInteger(fp);
	numCols = readFlippedInteger(fp);

	// printf("Magic number: %4x\n", magicNumber);
	//printf("Number of images: %d\n", numImages);
	//printf("Number of rows: %d\n", numRows);
	//printf("Number of columns: %d\n", numCols);

	fseek(fp2, 0x08, SEEK_SET);

	if(numImages > MAX_NUM_IMAGES) numImages = MAX_NUM_IMAGES;

	//////////////////////////////////////////////////////////////////
	// Go through each training data entry and figure out a
	// center for each digit

	int size = numRows*numCols;
	CvMat *trainingVectors = cvCreateMat(numImages, size, CV_32FC1);
	CvMat *trainingClasses = cvCreateMat(numImages, 1, CV_32FC1);

	memset(trainingClasses->data.ptr, 0, sizeof(float)*numImages);

	jbyte *temp = new jbyte[size];
	jbyte tempClass=0;
	for(int i=0;i<numImages;i++)
	{
		fread((void*)temp, size, 1, fp);
		fread((void*)(&tempClass), sizeof(jbyte), 1, fp2);

		trainingClasses->data.fl[i] = tempClass;

		// Normalize the vector
		/*float sumofsquares = 0;
		for(int k=0;k<size;k++)
			sumofsquares+=temp[k]*temp[k];
		sumofsquares = sqrt(sumofsquares);*/

		for(int k=0;k<size;k++)
			trainingVectors->data.fl[i*size+k] = temp[k]; ///sumofsquares;
	}

	knn->train(trainingVectors, trainingClasses);
	fclose(fp);
	fclose(fp2);

	return true;
}
///////////////////////////////////////////////////////////////////////////////

KNearest knn;
int knn_count = 0;

static int digit_count = 0;

const int train_samples = 1;
const int classes = 10;
const int sizex = 26;
const int sizey = 34;
const int ImageSize = sizex * sizey;
char pathToImages[] = "/sdcard/exartisan_pics/digit/";

void PreProcessImage(Mat *inImage,Mat *outImage,int sizex, int sizey);
void LearnFromImages(CvMat* trainData, CvMat* trainClasses);
void RunSelfTest(KNearest& knn2);
bool AnalyseImage(KNearest knearest);
/** @function main */
int digit_recog(/*Mat color_img*/)
{
	CvMat* trainData = cvCreateMat(classes * train_samples,ImageSize, CV_32FC1);
	CvMat* trainClasses = cvCreateMat(classes * train_samples, 1, CV_32FC1);

	LearnFromImages(trainData, trainClasses);
	KNearest knearest(trainData, trainClasses);
//	RunSelfTest(knearest);
	AnalyseImage(knearest);
//	AnalyseImage(knearest);

	cvReleaseMat( &trainData );
	cvReleaseMat( &trainClasses );
//	imwrite("/sdcard/digit/pic.jpg",color_img);

	return 0;
}

void PreProcessImage(Mat *inImage,Mat *outImage,int sizex, int sizey)
{
	Mat grayImage,blurredImage,thresholdImage,contourImage,regionOfInterest;

	vector<vector<Point> > contours;

	cvtColor(*inImage,grayImage , COLOR_BGR2GRAY);

	GaussianBlur(grayImage, blurredImage, Size(5, 5), 2, 2);
	adaptiveThreshold(blurredImage, thresholdImage, 255, 1, 1, 11, 2);

	thresholdImage.copyTo(contourImage);

	findContours(contourImage, contours, RETR_LIST, CHAIN_APPROX_SIMPLE);

	int idx = 0;
	size_t area = 0;
	for (size_t i = 0; i < contours.size(); i++)
	{
		if (area < contours[i].size() )
		{
			idx = i;
			area = contours[i].size();
		}
	}

	Rect rec = boundingRect(contours[idx]);

	regionOfInterest = thresholdImage(rec);

	resize(regionOfInterest,*outImage, Size(sizex, sizey));

}

void LearnFromImages(CvMat* trainData, CvMat* trainClasses)
{
	Mat img;
	char file[255];
	for (int i = 0; i < classes; i++)
	{
		sprintf(file, "%s/%d.png", pathToImages, i);
		img = imread(file, 1);
		if (!img.data)
		{
			exit(1);
		}
		Mat outfile;
		PreProcessImage(&img, &outfile, sizex, sizey);
		for (int n = 0; n < ImageSize; n++)
		{
			trainData->data.fl[i * ImageSize + n] = outfile.data[n];
		}
		trainClasses->data.fl[i] = i;
	}
}

void RunSelfTest(KNearest& knn2)
{
	Mat img;
	CvMat* sample2 = cvCreateMat(1, ImageSize, CV_32FC1);
	// SelfTest
	char file[255];
	int z = 0;
	while (z++ < 10)
	{
		int iSecret = rand() % 10;
		//cout << iSecret;
		sprintf(file, "%s/%d.png", pathToImages, iSecret);
		img = imread(file, 1);
		Mat stagedImage;
		PreProcessImage(&img, &stagedImage, sizex, sizey);
		for (int n = 0; n < ImageSize; n++)
		{
			sample2->data.fl[n] = stagedImage.data[n];
		}
		float detectedClass = knn2.find_nearest(sample2, 1);
		if (iSecret != (int) ((detectedClass)))
		{
			exit(1);
		}
		sprintf(file, "/sdcard/exartisan_pics/%d.png", iSecret);
		imwrite(file,img);
	}
}

int abc = 0;
char text[6000];
bool AnalyseImage(KNearest knearest)
{
	CvMat* sample2 = cvCreateMat(1, ImageSize, CV_32FC1);

	Mat image, gray, blur, thresh;

	vector < vector<Point> > contours;
	image = imread("/sdcard/exartisan_pics/answer_number_code.jpg", 1);
	if(!image.data) return 0;

	char file[255];
	Mat text_img(image.size(), CV_8U);
	rectangle(text_img,Rect(0,0,text_img.cols,text_img.rows),Scalar(255,255,255,255),CV_FILLED);

	cvtColor(image, gray, COLOR_BGR2GRAY);
	GaussianBlur(gray, blur, Size(5, 5), 2, 2);
	adaptiveThreshold(blur, thresh, 255, 1, 1, 11, 2);
	findContours(thresh, contours, RETR_LIST, CHAIN_APPROX_SIMPLE);

	for (size_t i = 0; i < contours.size(); i++)
	{
		vector < Point > cnt = contours[i];
		if (contourArea(cnt) > 50)
		{
			Rect rec = boundingRect(cnt);
			if ((rec.height > 27)&&(rec.height < 35)&&(rec.width < 28))
			{
				Mat roi = image(rec);
				Mat stagedImage;
				PreProcessImage(&roi, &stagedImage, sizex, sizey);

				for (int n = 0; n < ImageSize; n++)
				{
					sample2->data.fl[n] = stagedImage.data[n];
				}

				// bug in here.....
				float result = knearest.find_nearest(sample2, 1);

				//     float result = dr->classify(sample2);
				rectangle(image, Point(rec.x, rec.y),
						Point(rec.x + rec.width, rec.y + rec.height),
						Scalar(0, 0, 255), 2);
				if((rec.height/rec.width)>3)	result = 1;
				text_img.data[(10)*text_img.cols+rec.x] = round(result);

			}
		}
	}

	if (abc ==1)	imwrite("/sdcard/exartisan_pics/abc_1.png",image);

	int m=0;char tt;
	for (int k=0;k<text_img.rows;k++)
		for (int l=0;l<text_img.cols;l++)
		{
			if (text_img.data[k*text_img.cols+l]<10)
			{
				sprintf(&tt,"%d",text_img.data[k*text_img.cols+l]);
				text[m] = tt;
				m++;
			}
		}
	text[m] = '\0';

	putText(image, text, Point(50, 30), FONT_HERSHEY_PLAIN, 1.5, Scalar(255,100,100,255), 2.0);
	imwrite("/sdcard/exartisan_pics/answer_number_code_result.jpg", image);

	cvReleaseMat( &sample2 );

	if (m != 16)
	{
//		sprintf(text,"recognition failed!");
		return false;
	}
	return true;
}
// end test
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


#define Deg_PI 57.295779513082320876798154814105
// Japan 	B5	182x257
//			B5	176X250
//			A4	210X297

bool paper_flag=false, tree_black_flag=false, paper_inside_flag=false, head_tail_flag=false;
/**
 * calculate angle between pt1-pt0 and pt2-pt0
 * @param pt1 point 1
 * @param pt2 point 2
 * @param pt0 point root
 * @return angle in radian
 */

inline double angle( Point pt1, Point pt2, Point pt0 )
{
    double dx1 = pt1.x - pt0.x;
    double dy1 = pt1.y - pt0.y;
    double dx2 = pt2.x - pt0.x;
    double dy2 = pt2.y - pt0.y;
    return (dx1*dx2 + dy1*dy2)/sqrt((dx1*dx1 + dy1*dy1)*(dx2*dx2 + dy2*dy2) + 1e-10);
}

/**
 * calculate length Eclide between pt0 and pt1
 * @param pt1 point 1
 * @param pt0 point 2
 * @return length in logic
 */
inline double length( Point pt0, Point pt1)
{
	return (double) sqrt((pt0.x-pt1.x)*(pt0.x-pt1.x)+(pt0.y-pt1.y)*(pt0.y-pt1.y));
}

// no-reference blur metric
// The Blur Effect: Perception and Estimation with a New No-Reference Perceptual Blur Metric
// Frederique Cretea,b, Thierry Dolmierea, Patricia Ladreta, Marina Nicolasb
// Feb 2008
/*float blur_metric(Mat gray_img)
{
//	Mat _process_img(gray_img);
	Mat pyr;
	pyrDown(gray_img, pyr, Size(gray_img.cols/2, gray_img.rows/2));
	Mat _process_img(pyr);
	Mat Bver, Bhor;

}
*/

/**
 * calculate slope of camera
 * @param image is displayed image which display three angle slope of camera
 * @param obj is boundary of exam paper
 * @return none
 */
void slope_camera(Mat image,vector<Point> obj)
{
	double l01,l12,l23,l30;
	l01 = length (obj[0],obj[1]);
	l12 = length (obj[1],obj[2]);
	l23 = length (obj[2],obj[3]);
	l30 = length (obj[3],obj[0]);

	double angl1 = 2*Deg_PI*asin(fabs((l23-l01)/(l12+l30)));
	double angl2 = 2*Deg_PI*asin(fabs((l12-l30)/(l01+l23)));

	RotatedRect rotRect = minAreaRect(Mat(obj));

	char str[1024];
	sprintf(str,"slope(deg): (%d, %d), rotation(deg): %d",cvRound(angl1),cvRound(angl2),cvRound(fabs(rotRect.angle)));
	putText(image, str, Point(50, 50), FONT_HERSHEY_PLAIN, 1.2, Scalar(0,0,255,255), 2.0);

}

/**
 * transform trapezoid to rectangle
 * @param image is image have trapezoid exam paper
 * @param obj is vector array boundary exam paper
 * @return void
 */
void transform_image(Mat image, vector<Point> obj)
{
	Rect rect = boundingRect(obj);
	rect.height = rect.width*210/297; // keep A4

	Mat img;
	image.copyTo(img);
	Mat warp_matrix;
	Point2f srcQuad[4], dstQuad[4];

	srcQuad[0].x = obj[0].x;
	srcQuad[0].y = obj[0].y;
	srcQuad[1].x = obj[1].x;
	srcQuad[1].y = obj[1].y;
	srcQuad[2].x = obj[2].x;
	srcQuad[2].y = obj[2].y;
	srcQuad[3].x = obj[3].x;
	srcQuad[3].y = obj[3].y;

	if ((srcQuad[0].x-srcQuad[2].x)<0)
	{
		dstQuad[0].x = rect.x;
		dstQuad[0].y = rect.y;
		dstQuad[1].x = rect.x;
		dstQuad[1].y = rect.y+rect.height;
		dstQuad[2].x = rect.x+rect.width;
		dstQuad[2].y = rect.y+rect.height;
		dstQuad[3].x = rect.x+rect.width;
		dstQuad[3].y = rect.y;
	}
	else
	{
		dstQuad[0].x = rect.x+rect.width;
		dstQuad[0].y = rect.y;
		dstQuad[1].x = rect.x;
		dstQuad[1].y = rect.y;
		dstQuad[2].x = rect.x;
		dstQuad[2].y = rect.y+rect.height;
		dstQuad[3].x = rect.x+rect.width;
		dstQuad[3].y = rect.y+rect.height;
	}
	warp_matrix = getPerspectiveTransform(srcQuad,dstQuad);
//	Mat small_img (img.rows/8,img.cols/8,CV_8UC3);
//	resize(img,small_img,small_img.size());
	warpPerspective( img, image, warp_matrix,image.size());

//	rectangle(image,rect,Scalar(0,255,255,255),2);
}


/**
 * focus metric
 * @param gray_img is image have to measure
 * @return focus level in 0.0->1.0
 */
double contrast_measure(Mat gray_img)
{
	Mat cntrst(gray_img.rows/4, gray_img.cols/4, CV_8UC1);
	resize(gray_img,cntrst,cntrst.size());
//	pyrDown(gray_img, pyr, Size(gray_img.cols/2, gray_img.rows/2));
	Mat _process_img(cntrst);

	Mat lap_img(_process_img.rows,_process_img.cols,CV_32F);
	Mat integral_sum(_process_img.rows+1,_process_img.cols+1,CV_32F);
	Mat integral_sqsum(_process_img.rows+1,_process_img.cols+1,CV_64F);

//	countNonZero

	Laplacian(_process_img,lap_img,_process_img.depth());
	integral(lap_img,integral_sum,integral_sqsum,_process_img.depth());

	double sqsum = integral_sqsum.at<double>((integral_sqsum.rows-1),(integral_sqsum.cols-1));

	return sqsum/(_process_img.rows*_process_img.cols);
}

/**
 * find width of each edge point
 * @param img is gray scale image to measure
 * @ i is rows index of edge point
 * @ j is cols index of edge point
 * @return width of edge point
 */
int find_width_edge(Mat img, int i,int j)
{
	uchar *ptr = img.ptr<uchar>(i);
	int k = j,max,min;
//	if ptr[k-1]<ptr[k]<ptr[k+1]
	{
		while(k<img.cols-1)
		{
			if (ptr[k]>=ptr[k+1]) k++;
			else break;
		}
		min = k;
		k=j;
		while(k)
		{
			if(ptr[k-1]>=ptr[k]) k--;
			else break;
		}
		max = k;
	}
	int out = abs(max-min);
	//	if ptr[k-1]?ptr[k]>ptr[k+1]
	{
		while(k<img.cols-1)
		{
			if(ptr[k]<=ptr[k+1]) k++;
			else break;
		}
		max = k;
		k=j;
		while(k)
		{
			if(ptr[k-1]<=ptr[k]) k--;
			else break;
		}
		min = k;
	}
	return MAX(out,abs(max-min));
}
/**
 * blur metric
 * Marziliano [2002] A NO-REFERENCE PERCEPTUAL BLUR METRIC
 * @param gray_img is gray scale image have to measure
 * @return blur degree
 */
double blur_metric(Mat gray_img, Mat color_img)
{
	Mat _process_img(gray_img);
	Mat v_edge;
	Sobel(_process_img,v_edge,_process_img.depth(),1,0,3);
	double width_edge=0.0;
	int edge_number = 0;
	for(int i=0;i<v_edge.rows;i++)
	{
		uchar *ptr = v_edge.ptr<uchar>(i);
		for(int j=1;j<v_edge.cols-1;j++)
		{
			if (ptr[j]>30)
			{
				width_edge+=find_width_edge(_process_img,i,j);
				edge_number ++;
			}
		}
	}
	return double (width_edge/edge_number);
}


/**
 * find rectangle from contours vector
 * @param contours is contour of objects in input image
 * @param min_size is minimize size of objects that have to find
 * @param max_size is maximize size of objects that have to find
 * @param cosin is maximize cosin of angle of corner objects that have to find
 * @return vector array of contours of rectangle objects
 */
vector<vector<Point> > find_rectangle(vector<vector<Point> > contours, double min_size, double max_size, double cosin)
{
    vector<vector<Point> > squares;
    vector<Point> approx;

    double _area_max = 0.0;
    // test each contour
    for( size_t i = 0; i < contours.size(); i++ )
    {
    	// approximate contour with accuracy proportional
        // to the contour perimeter
        approxPolyDP(Mat(contours[i]), approx, arcLength(Mat(contours[i]), true)*0.02, true);

        // square contours should have 4 vertices after approximation
        // relatively large area (to filter out noisy contours)
        // and be convex.
        // Note: absolute value of an area is used because
        // area may be positive or negative - in accordance with the
        // contour orientation
        double _area =  fabs(contourArea(Mat(approx)));
        if( approx.size() == 4 && (_area > min_size) && (_area < max_size) && isContourConvex(Mat(approx)) )
        {
        	double maxCosine = 0;

            for( int j = 2; j < 5; j++ )
            {
            	// find the maximum cosine of the angle between joint edges
                double cosine = fabs(angle(approx[j%4], approx[j-2], approx[j-1]));
                maxCosine = MAX(maxCosine, cosine);
            }
             // if cosines of all angles are small
             // (all angles are ~90 degree) then write quandrange
             // vertices to resultant sequence
             if( maxCosine < cosin )
             {
            	  squares.push_back(approx);
             }
        }
    }
    return squares;
}


/////////test find three stone using minAreaRect and matchShapes
vector<vector<Point> > find_object(vector<vector<Point> > contours, double min_size, double max_size, double cosin)
{
    vector<vector<Point> > squares;
    vector<Point> approx;

    double _area_max = 0.0;
    // test each contour
    for( size_t i = 0; i < contours.size(); i++ )
    {
        approxPolyDP(Mat(contours[i]), approx, arcLength(Mat(contours[i]), true)*0.02, true);
//        RotatedRect rotRect = minAreaRect(Mat(contours[i]));
//        matchShapes
        double _area =  fabs(contourArea(Mat(approx)));
        if( (_area > min_size) && (_area < max_size) && isContourConvex(Mat(approx)) )
        {
//        	double maxCosine = 0;
//
//            for( int j = 2; j < 5; j++ )
//            {
//            	// find the maximum cosine of the angle between joint edges
//                double cosine = fabs(angle(approx[j%4], approx[j-2], approx[j-1]));
//                maxCosine = MAX(maxCosine, cosine);
//            }
//             if( maxCosine < cosin )
//             {
            	  squares.push_back(approx);
//             }
        }
    }

    return squares;
}
//////////////////

/////////////////////////////////////////////////////////////////////////////////
/**
 * find all rectangle from input image using cany method
 * @param image is input image
 * @return vector array of contours of rectangle objects
 */
vector<vector<Point> > find_all_rectangle( const Mat& gray_img, Mat color_img)
{
	vector<vector<Point> > squares;
	vector<vector<Point> > contours;

	Mat _process_img(gray_img);
	Canny(_process_img, _process_img, 100, 100, 5);

    // find contours and store them all as a list
    findContours(_process_img, contours, CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);

//    cvtColor(gray_img, color_img, CV_GRAY2RGB,4); // for debugging

    vector<Point> approx;
    // test each contour
    for( size_t i = 0; i < contours.size(); i++ )
    {
     	approxPolyDP(Mat(contours[i]), approx, arcLength(Mat(contours[i]), true)*0.02, true);

     	double _area = fabs(contourArea(Mat(approx)));
    	if( approx.size() == 4 && _area>100 && _area<40000000 && isContourConvex(Mat(approx)) )
    	{
    		double maxCosine = 0;
    		for( int j = 2; j < 5; j++ )
    		{
    			// find the maximum cosine of the angle between joint edges
    			double cosine = fabs(angle(approx[j%4], approx[j-2], approx[j-1]));
    			maxCosine = MAX(maxCosine, cosine);
    		}
    		if( maxCosine < 0.25 )
    			squares.push_back(approx);
    	}
    }

    for( size_t i = 0; i < squares.size(); i++ )
    {
     	const Point* p = &squares[i][0];
    	int n = (int)squares[i].size();
    	polylines(color_img, &p, &n, 1, true, Scalar(255,0,255,255), 3, CV_AA);
    }

    return squares;
}

//////////////////////////////////////////////////////


/**
 * find three black stone in exam paper
 * @param gray_img is input grayscale image
 * @param color_img is input color image for display
 * @return true if found three black stone that head and tail correct other is false
 */
bool find_black_stone(Mat gray_img, Mat color_img,Rect rect)
{
	Mat pyr;
	pyrDown(gray_img, pyr, Size(gray_img.cols/2, gray_img.rows/2));
	pyrUp(pyr, gray_img, Size(gray_img.cols, gray_img.rows));

	Mat _process_img(gray_img);

//	GaussianBlur(_process_img,_process_img, Size(5,5), 7, 7);
	threshold(_process_img,_process_img,70,255,THRESH_BINARY_INV);//THRESH_OTSU);


//	cvtColor(_process_img, color_img, CV_GRAY2RGB,4); // for debugging

    vector<vector<Point> > squares;
    vector<vector<Point> > contours;
    findContours(_process_img, contours, CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);
//    squares = find_rectangle(contours,100,4000,0.25);
    double min_size = 0.00012*rect.width*rect.height;//0.000085714*gray_img.cols*gray_img.rows;//0.000205714 10*9
    double max_size = 0.00075*rect.width*rect.height;//0.000342857*gray_img.cols*gray_img.rows; 19*11/488*690
    squares = find_object(contours,min_size,max_size,0.25); //15*8 ~

    if (squares.size() == 3)
    {
    	tree_black_flag = true;
    	Point2f pt0,pt1,pt2;
    	float r0,r1,r2;
    	minEnclosingCircle(squares[0],pt0,r0);
    	minEnclosingCircle(squares[1],pt1,r1);
    	minEnclosingCircle(squares[2],pt2,r2);
    	float l01, l02, l12;
    	l01 = sqrt((pt0.x-pt1.x)*(pt0.x-pt1.x)+(pt0.y-pt1.y)*(pt0.y-pt1.y));
    	l02 = sqrt((pt0.x-pt2.x)*(pt0.x-pt2.x)+(pt0.y-pt2.y)*(pt0.y-pt2.y));
    	l12 = sqrt((pt2.x-pt1.x)*(pt2.x-pt1.x)+(pt2.y-pt1.y)*(pt2.y-pt1.y));
    	float min_dis=min(l01,min(l02,l12));

    	bool h_t = false;
    	int tail = -1;
    	Point2f pt_tail;
    	if (min_dis == l01)
    	{
    		tail = 2; pt_tail = pt2;
    		if (pt2.x == max(pt2.x,max(pt1.x,pt0.x)))	h_t = true;
    	}
    	else if (min_dis == l02)
    	{
    		tail = 1; pt_tail = pt1;
    		if (pt1.x == max(pt2.x,max(pt1.x,pt0.x)))	h_t = true;
    	}
    	else if (min_dis == l12)
    	{
    		tail = 0; pt_tail = pt0;
    		if (pt0.x == max(pt2.x,max(pt1.x,pt0.x)))	h_t = true;
    	}

    	head_tail_flag = h_t;
    	circle(color_img,pt_tail,r0,Scalar(255,255,0,255),3);
    }

//	for debugging....
    for( size_t i = 0; i < squares.size(); i++ )
	{
    	const Point* p = &squares[i][0];
        int n = (int)squares[i].size();
        polylines(color_img, &p, &n, 1, true, Scalar(255,0,255,255), 3, CV_AA);

//        char str[1024];
//        sprintf(str,"%.1f",(contourArea(Mat(squares[i]))));
//        putText(color_img, str, p[0], FONT_HERSHEY_PLAIN, 1.2, Scalar(0,0,255,255), 2.0);
    }
//   cvtColor(_process_img, color_img, CV_GRAY2RGB,4);


//    for( size_t i = 0; i < contours.size(); i++ )
//    {
//       	const Point* p = &contours[i][0];
//       	int n = (int)contours[i].size();
//       	polylines(color_img, &p, &n, 1, true, Scalar(255,0,255,255), 3, CV_AA);
//       	char str[1024];
//       	sprintf(str,"%.1f -- %d",(contourArea(Mat(contours[i]))),rect.width*rect.height);
//       	putText(color_img, str, p[0], FONT_HERSHEY_PLAIN, 1.2, Scalar(0,0,255,255), 2.0);
//    }

	return head_tail_flag;
}


int pre_image_for_recog_counter = 0;
bool ocr_pre_flag = false;
//////////////////////////////////////////////////////////////
Rect pre_image_for_recog(Mat nc_img, int exam_width, bool & final)
{
	final = false;
	// rotate clockwise
	Mat nci_tmp;
	transpose(nc_img,nci_tmp);
	flip(nci_tmp,nci_tmp,1);

	Mat temp(nci_tmp.rows, nci_tmp.cols, CV_8UC1);

	nci_tmp.copyTo(temp);
	Mat _process_img(temp);
//	// contrast and brightness
//	double min_level, max_level;
//	minMaxLoc(_process_img,&min_level,&max_level);
//	double alpha = double (255.0/(max_level-min_level));
//	for( int y = 0; y < _process_img.rows; y++ )
//	for( int x = 0; x < _process_img.cols; x++ )
//	{
//		_process_img.at<uchar>(y,x) = saturate_cast<uchar>(round((double) alpha*( _process_img.at<uchar>(y,x)-min_level)));
//	}

	// contrast enhancement
	int alpha = 2, beta = -30;
	for( int y = 0; y < _process_img.rows; y++ )
	for( int x = 0; x < _process_img.cols; x++ )
	{
		_process_img.at<uchar>(y,x) = saturate_cast<uchar>( alpha*( _process_img.at<uchar>(y,x) ) + beta );
	}
	//
	double scale = 1860.0/exam_width;
	int rec_row = (int) round((double) scale*_process_img.rows);
	int rec_col = (int) round((double) scale*_process_img.cols);
	// resize for recognitin
	Mat nci(rec_row, rec_col, CV_8UC1);
	resize(_process_img,nci,nci.size());

	Mat blur_img, thresh;
	// find rectang boudary number code
	GaussianBlur(nci, blur_img, Size(5, 5), 2, 2);
	adaptiveThreshold(blur_img, thresh, 255, 1, 1, 11, 2);
	vector < vector<Point> > contours;
	findContours(thresh, contours, RETR_LIST, CHAIN_APPROX_SIMPLE);
	int rec_counter=0;
	Rect rec_out(0,0,500,40);
	for (size_t i = 0; i < contours.size(); i++)
	{
		vector < Point > cnt = contours[i];
		if (contourArea(cnt) > 50)
		{
			Rect rec = boundingRect(cnt);
			if ((rec.height > 38)&&(rec.width > 495))
			{
				rec_counter++;
				rectangle(nci,rec,Scalar(255,255,255,255),9);
				rec_out = rec;
			}
		}
	}

	if (rec_counter > 0)
	{
		Mat nci_out = nci(rec_out);
		imwrite("/sdcard/exartisan_pics/answer_number_code.jpg", nci_out);
		final = true;
	}
	rec_out.x = round((double)(rec_out.x/scale));
	rec_out.y = round((double)(rec_out.y/scale));
	rec_out.width = round((double)(rec_out.width/scale));
	rec_out.height = round((double)(rec_out.height/scale));

//	char file[255];
//	sprintf(file, "/sdcard/exartisan_pics/number_code%d.png",pre_image_for_recog_counter++);
//	if (rec_counter == 2)	imwrite(file,nci);
	ocr_pre_flag = final;
	return rec_out;
}

//////////////////////////////////////////////////////////////

bool number_recognition_flag = true;

/**
 * find exam paper
 * @param gray_img is input grayscale image
 * @param color_img is input color image for display
 * @return true if detected exam paper
 */
int exam_paper_dectect(Mat gray_img, Mat color_img)
{
	int detect = 0;
	int ratio = 3;

    Mat pyr(gray_img.rows/ratio, gray_img.cols/ratio, CV_8UC1);
    resize(gray_img,pyr,pyr.size());
    Mat _process_img(pyr);
    Canny(_process_img, _process_img, 100, 100, 3);
//    dilate(_process_img, _process_img,Mat());

    vector<vector<Point> > squares;
    vector<vector<Point> > contours;
    findContours(_process_img, contours, CV_RETR_LIST, CV_CHAIN_APPROX_SIMPLE);
    squares = find_rectangle(contours,2000.0,(_process_img.rows-5)*(_process_img.cols-5),0.25);

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

//    		// if detected exam paper then detect black stone position
//    		// create image mask
//    		Mat mask(gray_img.rows, gray_img.cols, CV_8UC1);
//    		rectangle(mask,Rect(0,0,gray_img.cols,gray_img.rows),Scalar(0,0,0,0),CV_FILLED);
//    		rectangle(mask,rect,Scalar(255,255,255,255),CV_FILLED);
//        	// create temp image
//        	Mat temp(gray_img.rows, gray_img.cols, CV_8UC1);
//        	rectangle(temp,Rect(0,0,gray_img.cols,gray_img.rows),Scalar(255,255,255,255),CV_FILLED);
//        	gray_img.copyTo(temp, mask);
//
//
//
//        	Mat answer_reg_image(gray_img.rows, gray_img.cols, CV_8UC1);
//        	temp.copyTo(answer_reg_image);
//
//        	// find three black stone
//        	rectangle(temp,Rect(rect.x,rect.y+rect.height/5,rect.width,3*rect.height/5),Scalar(255,255,255,255),CV_FILLED);
//        	//cvtColor(temp, color_img, CV_GRAY2RGB,4); // for debugging
//        	if (find_black_stone(temp,color_img,rect)) detect = 1;
//        	else detect = 0;
//
//        	if (detect)
//        	{
//        		// answer number code
//        		Rect number_code_reg(rect.x+0.12*rect.width,rect.y+0.23*rect.height, 0.038*rect.width,0.42*rect.height);
//        		Mat number_code_image = answer_reg_image(number_code_reg);
//        		bool final;
//        		Rect nci_rect = pre_image_for_recog(number_code_image, rect.width, final);
//        		// Number code Recognition
//        		if (final)
//        		{
//        			if (number_recognition_flag)
//        			{
//        				//digit_recog();
//        				//AnalyseImage(knn);
//        				number_recognition_flag = !AnalyseImage(knn);
//        				number_recognition_flag = false;
//        			}
//        			number_code_reg.x += nci_rect.y;
//        			number_code_reg.y += nci_rect.x;
//        			number_code_reg.height = nci_rect.width;
//        			number_code_reg.width = nci_rect.height;
//        		}
//        		// draw white rectangle number code position
//        		rectangle(color_img,number_code_reg,Scalar(255,255,255,255),1);
//        		// draw white rectangle answer region
//        		Rect answer_rect(rect.x+rect.width/5+5,rect.y+0.03*rect.height,4*rect.width/5-0.025*rect.height, rect.height-0.055*rect.height);
//        		rectangle(color_img,answer_rect,Scalar(255,255,255,255),1);
//
//        		// write exam paper image
//        		if (!number_recognition_flag)
//        		{
//        			putText(color_img, text, Point(answer_rect.x+10, answer_rect.y+30), CV_FONT_HERSHEY_SIMPLEX, 0.7, Scalar(255,255,255,255), 1.5);
//        			char file[255];
//        			sprintf(file, "/sdcard/exartisan_pics/exam_paper%d.jpg",pre_image_for_recog_counter++);
//        			Mat exam_img = color_img(rect);
//        			imwrite(file,exam_img);
//        			Mat gray_exam_img;
//        			cvtColor(exam_img,gray_exam_img,CV_RGB2GRAY);
//        			sprintf(file, "/sdcard/exartisan_pics/gray_exam_paper%d.jpg",pre_image_for_recog_counter++);
//        			imwrite(file,gray_exam_img);
//        		}
//        	}
//        	cvtColor(answer_reg_image, color_img, CV_GRAY2RGB,4); // for debugging

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

    return detect;
}

bool snap_image_enable = true;
bool snap_image_processing()
{
	Mat image = imread("/sdcard/exartisan_pics/ima.jpg", 1);
	if(!image.data) return 0;

	// Starting ...
	double t = (double)getTickCount();

//	Mat gray_image;
//	cvtColor(image, gray_image, CV_RGB2GRAY);

	Mat color_img(480, 800, CV_8UC3);
	Mat gray_img(480, 800, CV_8UC1);
	resize(image,color_img,color_img.size());
	cvtColor(color_img, gray_img, CV_RGB2GRAY);

	exam_paper_dectect(gray_img,color_img);

	// do something ...
    t = ((double)getTickCount() - t)/getTickFrequency();
    char str[1024];
	sprintf(str,"Complete at %.2f second, ",t);
	putText(color_img, str, Point(100, 100), FONT_HERSHEY_PLAIN, 3, Scalar(0,0,255,255), 2.0);

//	imwrite("/sdcard/exartisan_pics/paper_gray.jpg",gray_image); //tiff
	imwrite("/sdcard/exartisan_pics/paper_color.jpg",color_img);

	return 1;
}

/**
 * function called from Java
 * @param env is env of Java
 * @param width is width of input image
 * @param height is height of input image
 * @param yuv is input image
 * @param bgra is displayed color image
 * @param bgra2 is saved image
 * @param grayFlag is flag for saved grayscale image
 * @return true if input image is exam paper correct
 */
extern "C" {
JNIEXPORT int JNICALL Java_com_artisan_apps_blab_LiveCameraView_FindFeatures(JNIEnv* env, jobject, jint width, jint height, jbyteArray yuv, jintArray bgra, jintArray bgra2, bool grayFlag, bool ocrFlag)
{

	knn_count = 1;// enable for not training database
		if (width==0)
		{
					if(knn_count==0)
					{
						knn_count++;

						CvMat* trainData = cvCreateMat(classes * train_samples,ImageSize, CV_32FC1);
						CvMat* trainClasses = cvCreateMat(classes * train_samples, 1, CV_32FC1);

						LearnFromImages(trainData, trainClasses);
						knn.train(trainData, trainClasses);

//						Mat training_database_image = imread("/sdcard/exartisan_pics/digit/buchstaben.png", 1);
//						imwrite("/sdcard/exartisan_pics/training_database.png", training_database_image);
					}

//				number_recognition_flag = true;
//				sprintf(text,"recognize again...!");
				return 0;
			}

		// snap image processing
//		if (snap_image_enable)
//		{
//			snap_image_enable = false;
//			snap_image_processing();
//			return 0;
//		}

		paper_flag=false; tree_black_flag=false; paper_inside_flag=false; head_tail_flag=false;
		int snap = 0;
		jbyte* _yuv  = env->GetByteArrayElements(yuv, 0);
	    jint*  _bgra = env->GetIntArrayElements(bgra, 0);
	    jint*  _bgra2 = env->GetIntArrayElements(bgra2, 0);

	    Mat myuv(height + height/2, width, CV_8UC1, (unsigned char *)_yuv);
	    Mat mbgra(height, width, CV_8UC4, (unsigned char *)_bgra);
	    Mat out_img(height, width, CV_8UC4, (unsigned char *)_bgra2);
	    Mat mgray(height, width, CV_8UC1, (unsigned char *)_yuv);

	    //Please make attention about BGRA byte order
	    //ARGB stored in java as int array becomes BGRA at native level
	    cvtColor(myuv, mbgra, CV_YUV420sp2BGR, 4);
	    cvtColor(myuv, out_img, CV_YUV420sp2BGR, 4);

	    // Starting ...
	    double t = (double)getTickCount();

	    Mat  grayimage;
	    mgray.copyTo(grayimage);

	    double min_level, max_level;
	    minMaxLoc(grayimage,&min_level,&max_level);

//	    find_all_rectangle(grayimage,mbgra);

	    mgray.copyTo(grayimage);
	    snap = exam_paper_dectect (grayimage,mbgra);
	    if (ocr_pre_flag) snap = 2;//ocrFlag = true;//
//
	    double contrast=0.0;
//	    mgray.copyTo(grayimage);
//	    contrast = blur_metric(grayimage,mbgra);

	    if (contrast>6) snap = 3;

//	    if (!number_recognition_flag)
//	    {
//	    	number_recognition_flag = true;
//	    }

//	    vector<vector<Point> > squares;
//	    squares = findSquares(grayimage);

//	    for( size_t i = 0; i < squares.size(); i++ )
//	    {
//	    	const Point* p = &squares[i][0];
//	    	int n = (int)squares[i].size();
//	    	polylines(mbgra, &p, &n, 1, true, Scalar(0,0,255,255), 3, CV_AA);
//	    }

//	    String filename = getFilesDir() + "/pics.jpg";

/*	    if (snap)
	    {
	    	mgray.copyTo(grayimage);
	        contrast = contrast_measure(grayimage);
	        char str[1024];
	        sprintf(str,"contrast:%.3f",contrast);
	        putText(mbgra, str, Point(50, 80), FONT_HERSHEY_PLAI N, 1.2, Scalar(0,0,255,255), 2.0);
	    }
*/
	    // do something ...
	    t = ((double)getTickCount() - t)/getTickFrequency();
//	    char str[1024];
//		sprintf(str,"%d, %d Complete at %.2f fps, ",mbgra.rows,mbgra.cols,1/t);
//		putText(mbgra, str, Point(20, 20), FONT_HERSHEY_PLAIN, 3, Scalar(0,0,255,255), 1.0);


	    char str[1024];
	    sprintf(str,"(w,h)=(%d,%d), ctrs=%.3f, br=(%.0f-%.0f), fps=%.1f, fc=%d",
	    		width, height,contrast, min_level,max_level,1/t,digit_count);
	    putText(mbgra, str, Point(50, 50), FONT_HERSHEY_PLAIN, 1.2, Scalar(0,0,255,255), 2.0);


	    if(paper_flag&&paper_inside_flag&&tree_black_flag&&head_tail_flag)
	    {
	    	circle(mbgra,Point(50,mbgra.rows/2),25,Scalar(0,255,255,255),CV_FILLED);
	    	rectangle(mbgra,Point(50,mbgra.rows/2+50),Point(100,mbgra.rows/2-50),Scalar(0,0,255,255),CV_FILLED);
	    }
	    else if (!tree_black_flag)
	    {
	    	rectangle(mbgra,Point(50,mbgra.rows/2+50),Point(100,mbgra.rows/2-50),Scalar(0,0,255,255),CV_FILLED);
	    }
	    else if (!head_tail_flag)
	    {
	    	circle(mbgra,Point(50,mbgra.rows/2),25,Scalar(0,255,255,255),CV_FILLED);
	    	line(mbgra,Point(30,mbgra.rows/2-20),Point(70,mbgra.rows/2+20),Scalar(0,0,255,255),5);
	    	line(mbgra,Point(30,mbgra.rows/2+20),Point(70,mbgra.rows/2-20),Scalar(0,0,255,255),5);
	    	rectangle(mbgra,Point(50,mbgra.rows/2+50),Point(100,mbgra.rows/2-50),Scalar(0,0,255,255),CV_FILLED);
	    }
	    else
	    {
	    	line(mbgra,Point(30,mbgra.rows/2-20),Point(70,mbgra.rows/2+20),Scalar(0,0,255,255),5);
	    	line(mbgra,Point(30,mbgra.rows/2+20),Point(70,mbgra.rows/2-20),Scalar(0,0,255,255),5);
	    }

//	    if (snap) imwrite("/sdcard/pic1.jpg",out_img);

	    // grayscale
	    if (grayFlag)
	    {
	    	cvtColor(mgray, out_img, CV_GRAY2RGB,4);
//	    	imwrite("/sdcard/pic1.jpg",out_img);
	    }

// test module digit recognition using KNearest
	    digit_count++;
	   	if (digit_count == 10)
//	   	{
////	   		digit_recog();
//	   	    AnalyseImage(knn);
//	   	}
//	   	if (digit_count == 20)
//	   	{
//	   		abc = 1;
//	   		AnalyseImage(knn);
//	   	}
//	   	putText(mbgra, text, Point(50, 100), FONT_HERSHEY_PLAIN, 1.5, Scalar(0,0,255,255), 3.0);
//	   	putText(mbgra, text, Point(50, 100), FONT_HERSHEY_PLAIN, 1.5, Scalar(255,255,255,255), 1.5);
// end test module

	    env->ReleaseIntArrayElements(bgra, _bgra, 0);
	    env->ReleaseIntArrayElements(bgra2, _bgra2, 0);
	    env->ReleaseByteArrayElements(yuv, _yuv, 0);

	    return snap;
}
}
