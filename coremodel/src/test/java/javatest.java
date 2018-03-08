import java.util.Random;

public class javatest {


    public static void QuickSort1(int[] arr,int left,int right){
        if(left >= right)
            return;
        int tmp ;
        int p = arr[left];
        int low = left;
        int high = right;
        while(low < high){
            while(low < high && p <= arr[high])
                high--;
            while(low < high && p >= arr[low])
                low++;
            if(low < high){
                tmp = arr[low];
                arr[low] = arr[high];
                arr[high] = tmp;
            }
        }
        arr[low] = p;
        QuickSort1(arr,left,low-1);
        QuickSort1(arr,low+1,right);

    }

    public static void QuickSort2(int[] arr,int left,int right){
        if(left >= right)
            return;
        int p = arr[left];
        int low = left;
        int high = right;
        while(low < high){
            while(low < high && p <= arr[high])
                high--;
            arr[low] = arr[high];
            while(low < high && p >= arr[low])
                low++;
           arr[high] = arr[low];
        }
        arr[low] = p;
        QuickSort2(arr,left,low-1);
        QuickSort2(arr,low+1,right);
    }


    public static void bubbleSort(int[] arr){
        int tmp ;
        for (int i = 0;i < arr.length ; i++)
            for(int j = 0 ; j< arr.length-1-i ;j++){
                if(arr[j] > arr[j+1]){
                    tmp = arr[j];
                    arr[j] = arr[j+1];
                    arr[j+1] = tmp;
                }
            }
    }


    public static void selectSort(int[] arr){
        int tmp,minIndex;
        for (int i = 0;i < arr.length-1 ; i++){
            minIndex = i;
            for(int j = i+1 ; j< arr.length;j++){
                if(arr[minIndex] > arr[j]){
                   minIndex = j;
                }
            }
            if(minIndex != i){
                tmp = arr[minIndex];
                arr[minIndex] = arr[i];
                arr[i] = tmp;
            }
        }
    }

    public static void heapSort(int[] arr){
        int tmp = 0;
        int lastIndex = arr.length-1;
        for(int i = 0;i< lastIndex;i++){
            buildHeap(arr,lastIndex-i);
            tmp = arr[0];
            arr[0] = arr[lastIndex-i];
            arr[lastIndex-i] = tmp;
        }

    }

    public static void buildHeap(int[] arr,int lastIndex){
        int tmp = 0;
        int k = 0;//定位当前父节点
        for(int i = (lastIndex-1)/2;i >= 0 ; i--){
            k = i;
            int biggerIndex = 2 * k + 1;
            if (biggerIndex + 1 < lastIndex &&  arr[biggerIndex] < arr[biggerIndex + 1] ) {
                biggerIndex++;
            }
            if (arr[k] < arr[biggerIndex]) {
                tmp = arr[biggerIndex];
                arr[biggerIndex] = arr[k];
                arr[k] = tmp;
            }
        }
    }


    public static void main(String[] args) {
//        int[] arrs = new int[20];
//        for(int i = 0; i < arrs.length;i++){
//            arrs[i] = new Random().nextInt(arrs.length);
//        }

        int[] arrs = {3,4,6,2,44,1,6,2,3,5,2,43,25,2,3,45,3,42};

        long star = System.currentTimeMillis();
        heapSort(arrs);
        long end = System.currentTimeMillis();
        System.out.println(end - star);

        //          2       1
        //20000000  3689
        for (int a:arrs) {
            System.out.print(a+"\t");
        }
    }
}
