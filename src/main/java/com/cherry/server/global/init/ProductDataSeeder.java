package com.cherry.server.global.init;

import com.cherry.server.product.domain.Product;
import com.cherry.server.product.domain.ProductStatus;
import com.cherry.server.product.domain.TradeType;
import com.cherry.server.product.repository.ProductRepository;
import com.cherry.server.user.domain.User;
import com.cherry.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class ProductDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (productRepository.count() > 0) {
            return;
        }

        User user = User.builder()
                .email("test@cherry.com")
                .nickname("CherrySeller")
                .profileImageUrl("https://via.placeholder.com/50")
                .build();
        userRepository.save(user);

        IntStream.rangeClosed(1, 1000).forEach(i -> {
            Product product = Product.builder()
                    .seller(user)
                    .title("아이돌 굿즈 상품 " + i)
                    .description("상품 설명입니다. #" + i)
                    .price(10000 + (i * 100))
                    .status(ProductStatus.SELLING)
                    .tradeType(TradeType.DELIVERY)
                    .build();
            productRepository.save(product);
        });
    }
}
