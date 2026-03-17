package org.example.bt4;

import org.example.bt4.model.Book;
import org.example.bt4.service.BookService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Scanner;

@Component
public class BookConsoleApp implements CommandLineRunner {

    private final BookService bookService;
    private final Scanner scanner = new Scanner(System.in);

    public BookConsoleApp(BookService bookService) {
        this.bookService = bookService;
    }

    @Override
    public void run(String... args) {
        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = Integer.parseInt(scanner.nextLine());

            if (choice == 0) {
                running = false;
                System.out.println("Đang thoát chương trình...");
                System.exit(0);
                continue;
            }

            System.out.print("Nhập loại DB (MySql, Mongo, Redis, Influx): ");
            String dbType = scanner.nextLine();

            try {
                handleChoice(choice, dbType);
            } catch (Exception e) {
                System.err.println("Lỗi: " + e.getMessage());
            }
        }
    }

    private void printMainMenu() {
        System.out.println("\n========= MENU QUẢN LÝ SÁCH =========");
        System.out.println("1. Lưu sách mới");
        System.out.println("2. Tìm kiếm sách");
        System.out.println("3. Cập nhật sách");
        System.out.println("4. Xóa sách (theo danh sách ID)");
        System.out.println("5. Thống kê theo tác giả");
        System.out.println("6. Xem tất cả (Phân trang)");
        System.out.println("0. Thoát");
        System.out.print("Lựa chọn của bạn: ");
    }

    private void handleChoice(int choice, String dbType) {
        switch (choice) {
            case 1 -> {
                Book book = new Book();
                System.out.print("Nhập tác giả: "); book.setAuthor(scanner.nextLine());
                System.out.print("Nhập thể loại: "); book.setCategory(scanner.nextLine());
                System.out.print("Nhập tên sách: "); book.setTitle(scanner.nextLine());
                System.out.print("Nhập nội dung: "); book.setContent(scanner.nextLine());
                System.out.print("Nhập số lượt xem: "); book.setViewCount(Long.parseLong(scanner.nextLine()));
                System.out.print("Nhập số lượt tải: "); book.setDownloadCount(Long.parseLong(scanner.nextLine()));
                bookService.saveBookToSpecificDB(book, dbType);
                System.out.println("Lưu thành công!");
            }
            case 2 -> {
                System.out.print("Từ khóa tên: "); String title = scanner.nextLine();
                System.out.print("Từ khóa tác giả: "); String author = scanner.nextLine();
                System.out.print("Từ khóa nội dung: "); String content = scanner.nextLine();
                bookService.searchBooks(dbType, title, author, content, 0, 10);
                System.out.println("Đã thực hiện tìm kiếm.");
            }
            case 4 -> {
                System.out.print("Nhập các ID cách nhau bởi dấu phẩy: ");
                String ids = scanner.nextLine();
                bookService.deleteBooks(dbType, Arrays.asList(ids.split(",")));
                System.out.println("Đã gửi yêu cầu xóa.");
            }
            case 5 -> {
                System.out.print("Nhập tên tác giả: ");
                String author = scanner.nextLine();
                var stats = bookService.statisticByAuthor(dbType, author);
                System.out.println("Kết quả thống kê: " + stats);
            }
            case 6 -> {
                var books = bookService.findAllPaging(dbType, 0, 10);
                books.forEach(System.out::println);
            }
            default -> System.out.println("Lựa chọn không hợp lệ.");
        }
    }
}
