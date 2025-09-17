package com.HLDLLD.creational;

interface Gpu{
    void assemble();
}

  class Nsi implements Gpu{
    @Override
    public void assemble(){
        //
    }
}

 class Asu implements Gpu{
    @Override
    public void assemble(){

    }
}

 abstract class Company{
    Gpu Assemble(){
        Gpu gpu = createGpu();
        gpu.assemble();
        return gpu;
    }
    abstract Gpu createGpu();
}

class NsiMan extends  Company{

    @Override
    Gpu createGpu(){
        return new Nsi();
    }
}
class Asuan extends  Company{

    @Override
    Gpu createGpu(){
        return new Asu();
    }
}

 class AbstractFactory {


     public static void main(String[] args) {
         System.out.println();
         Company nsi = new NsiMan();
         nsi.Assemble();
         Company asu = new Asuan();
         asu.Assemble();

     }
 }



 /* conmpany - abstract factory

    nsiman -> concrete factory 1
    asuman -> concrete factory 2

    nsigpu -> conccrete product a1
    nsimonitor -> conccrete product b1
    asugpu -> conccrete product a2
    asumonitor -> conccrete product b2

    gpu  -> interface a
    monitor -> interface b



  */
